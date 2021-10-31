package cum.xiaro.trollhack.module.modules.combat

import cum.xiaro.trollhack.util.extension.fastCeil
import cum.xiaro.trollhack.util.extension.sq
import cum.xiaro.trollhack.util.graphics.ColorRGB
import cum.xiaro.trollhack.event.SafeClientEvent
import cum.xiaro.trollhack.event.events.RunGameLoopEvent
import cum.xiaro.trollhack.event.events.TickEvent
import cum.xiaro.trollhack.event.events.render.Render2DEvent
import cum.xiaro.trollhack.event.events.render.RenderEntityEvent
import cum.xiaro.trollhack.event.listener
import cum.xiaro.trollhack.event.safeListener
import cum.xiaro.trollhack.manager.managers.CombatManager
import cum.xiaro.trollhack.manager.managers.EntityManager
import cum.xiaro.trollhack.manager.managers.FriendManager
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.module.modules.player.AutoEat
import cum.xiaro.trollhack.process.PauseProcess
import cum.xiaro.trollhack.process.PauseProcess.pauseBaritone
import cum.xiaro.trollhack.process.PauseProcess.unpauseBaritone
import cum.xiaro.trollhack.util.*
import cum.xiaro.trollhack.util.EntityUtils.eyePosition
import cum.xiaro.trollhack.util.EntityUtils.flooredPosition
import cum.xiaro.trollhack.util.EntityUtils.isHostile
import cum.xiaro.trollhack.util.EntityUtils.isNeutral
import cum.xiaro.trollhack.util.EntityUtils.isPassive
import cum.xiaro.trollhack.util.EntityUtils.isTamed
import cum.xiaro.trollhack.util.combat.CalcContext
import cum.xiaro.trollhack.util.combat.CrystalUtils
import cum.xiaro.trollhack.util.combat.MotionTracker
import cum.xiaro.trollhack.util.combat.SurroundUtils
import cum.xiaro.trollhack.util.graphics.GlStateUtils
import cum.xiaro.trollhack.util.graphics.ProjectionUtils
import cum.xiaro.trollhack.util.graphics.RenderUtils2D
import cum.xiaro.trollhack.util.graphics.RenderUtils3D
import cum.xiaro.trollhack.util.graphics.color.setGLColor
import cum.xiaro.trollhack.util.math.RotationUtils.getRelativeRotation
import cum.xiaro.trollhack.util.math.vector.Vec2d
import cum.xiaro.trollhack.util.math.vector.distanceTo
import cum.xiaro.trollhack.util.threads.defaultScope
import cum.xiaro.trollhack.util.threads.isActiveOrFalse
import cum.xiaro.trollhack.util.threads.runSafeOrFalse
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemFood
import net.minecraft.item.ItemPickaxe
import net.minecraft.util.EnumHand
import org.lwjgl.opengl.GL11.*

internal object CombatSetting : Module(
    name = "CombatSetting",
    description = "Settings for combat module targeting",
    category = Category.COMBAT,
    visible = false,
    alwaysEnabled = true
) {
    private val page = setting("Page", Page.TARGET_TYPE)

    /* Target Type */
    private val players = setting("Players", true, page.atValue(Page.TARGET_TYPE))
    private val friends by setting("Friends", false, page.atValue(Page.TARGET_TYPE) and players.atTrue())
    private val teammate by setting("Teammate", false, page.atValue(Page.TARGET_TYPE) and players.atTrue())
    private val mobs = setting("Mobs", true, page.atValue(Page.TARGET_TYPE))
    private val passive by setting("Passive", false, page.atValue(Page.TARGET_TYPE) and mobs.atTrue())
    private val neutral by setting("Neutral", false, page.atValue(Page.TARGET_TYPE) and mobs.atTrue())
    private val hostile by setting("Hostile", false, page.atValue(Page.TARGET_TYPE) and mobs.atTrue())
    private val tamed by setting("Tamed", false, page.atValue(Page.TARGET_TYPE) and mobs.atTrue())
    private val invisible by setting("Invisible", true, page.atValue(Page.TARGET_TYPE))
    private val ignoreWalls by setting("Ignore Walls", false, page.atValue(Page.TARGET_TYPE))
    private val targetRange by setting("Target Range", 16.0f, 4.0f..64.0f, 2.0f, page.atValue(Page.TARGET_TYPE))
    private val wallRange by setting("Wall Range", 3.0f, 0.0f..6.0f, 0.1f, page.atValue(Page.TARGET_TYPE))

    /* Target Priority */
    private val distancePriority by setting("Distance Priority", 0.5f, 0.0f..1.0f, 0.01f, page.atValue(Page.TARGET_PRIORITY))
    private val healthPriority by setting("Health Priority", 0.5f, 0.0f..1.0f, 0.01f, page.atValue(Page.TARGET_PRIORITY))
    private val armorPriority by setting("Armor Priority", 0.5f, 0.0f..1.0f, 0.01f, page.atValue(Page.TARGET_PRIORITY))
    private val holePriority by setting("Hole Priority", 0.5f, 0.0f..1.0f, 0.01f, page.atValue(Page.TARGET_PRIORITY))
    private val crosshairPriority by setting("Crosshair Priority", 0.5f, 0.0f..1.0f, 0.01f, page.atValue(Page.TARGET_PRIORITY))

    /* In Combat */
    private val pauseForDigging = setting("Pause For Digging", false, page.atValue(Page.IN_COMBAT))
    private val pauseForEating = setting("Pause For Eating", false, page.atValue(Page.IN_COMBAT))
    private val ignoreOffhandEating = setting("Ignore Offhand Eating", true, page.atValue(Page.IN_COMBAT) and pauseForEating.atTrue())
    private val pauseBaritone = setting("Pause Baritone", true, page.atValue(Page.IN_COMBAT))
    private val resumeDelay = setting("Resume Delay", 3, 1..10, 1, page.atValue(Page.IN_COMBAT) and pauseBaritone.atTrue())

    /* Calculation */
    val assumeResistance by setting("Assume Resistance", true, page.atValue(Page.CALCULATION))
    private val motionPredict = setting("Motion Predict", true, page.atValue(Page.CALCULATION))
    private val pingSync = setting("Ping Sync", false, page.atValue(Page.CALCULATION) and motionPredict.atTrue())
    private val ticksAhead by setting("Ticks Ahead", 6, 1..20, 1, page.atValue(Page.CALCULATION) and motionPredict.atTrue() and pingSync.atFalse())
    private val selfPredict = setting("Self Predict", true, page.atValue(Page.CALCULATION) and motionPredict.atTrue())
    private val pingSyncSelf = setting("Ping Sync Self", false, page.atValue(Page.CALCULATION) and motionPredict.atTrue() and selfPredict.atTrue())
    private val ticksAheadSelf by setting("Ticks Ahead Self", 3, 1..20, 1, page.atValue(Page.CALCULATION) and motionPredict.atTrue() and selfPredict.atTrue() and pingSyncSelf.atFalse())
    val crystalUpdateDelay by setting("Crystal Update Delay", 25, 5..500, 1, page.atValue(Page.CALCULATION))
    val horizontalCenterSampling by setting("Horizontal Center Sampling", false, page.atValue(Page.CALCULATION)).apply { listeners.add(CalcContext::resetSamplePoints) }
    val verticalCenterSampling by setting("Vertical Center Sampling", true, page.atValue(Page.CALCULATION)).apply { listeners.add(CalcContext::resetSamplePoints) }
    val backSideSampling by setting("Back Side Sampling", true, page.atValue(Page.CALCULATION))

    /* Render */
    private val renderPrediction = setting("Render Prediction", true, page.atValue(Page.RENDER) and motionPredict.atTrue())
    private val chams0 = setting("Chams", true, page.atValue(Page.RENDER))
    val chams by chams0
    private val chamsColor by setting("Chams Color", ColorRGB(255, 32, 255, 127), true, page.atValue(Page.RENDER) and chams0.atTrue())

    private enum class Page {
        TARGET_TYPE, TARGET_PRIORITY, IN_COMBAT, CALCULATION, RENDER
    }

    private var overrideRange = targetRange
    private var paused = false
    private val resumeTimer = TickTimer(TimeUnit.SECONDS)

    private var job: Job? = null
    private val timer = TickTimer()

    val pause
        get() = runSafeOrFalse {
            player.ticksExisted < 10
                || checkDigging()
                || checkEating()
        }

    private fun SafeClientEvent.checkDigging() =
        pauseForDigging.value
            && player.heldItemMainhand.item is ItemPickaxe
            && playerController.isHittingBlock

    private fun SafeClientEvent.checkEating() =
        pauseForEating.value
            && (PauseProcess.isPausing(AutoEat) || player.isHandActive && player.activeItemStack.item is ItemFood)
            && (!ignoreOffhandEating.value || player.activeHand != EnumHand.OFF_HAND)

    override fun isActive() = KillAura.isActive() || BedAura.isActive() || TrollAura.isActive() || Surround.isActive()

    init {
        listener<RenderEntityEvent.Model.Pre> {
            if (it.cancelled || !chams || it.entity != CombatManager.target) return@listener

            glDepthRange(0.0, 0.01)
            chamsColor.setGLColor()
            GlStateUtils.texture2d(false)
            GlStateUtils.lighting(false)
            GlStateUtils.blend(true)
            GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO)
        }
        listener<RenderEntityEvent.Model.Post> {
            if (it.cancelled || !chams || it.entity != CombatManager.target) return@listener

            glColor4f(1.0f, 1.0f, 1.0f, 1.0f)
            GlStateUtils.texture2d(true)
            GlStateUtils.lighting(true)
        }

        listener<RenderEntityEvent.All.Post> {
            if (!it.cancelled && chams && it.entity == CombatManager.target) {
                glDepthRange(0.0, 1.0)
            }
        }

        listener<Render2DEvent.Mc> {
            if (!motionPredict.value || !renderPrediction.value) return@listener

            CombatManager.target?.let {
                val ticks = getPredictTicksTarget()

                val posCurrent = EntityUtils.getInterpolatedPos(it, RenderUtils3D.partialTicks)
                val posAhead = CombatManager.trackerTarget?.calcPosAhead(ticks, true) ?: return@listener

                val posAheadEye = posAhead.add(0.0, it.eyeHeight.toDouble(), 0.0)
                val posCurrentScreen = Vec2d(ProjectionUtils.toScaledScreenPos(posCurrent)).toVec2f()
                val posAheadScreen = Vec2d(ProjectionUtils.toScaledScreenPos(posAhead)).toVec2f()
                val posAheadEyeScreen = Vec2d(ProjectionUtils.toScaledScreenPos(posAheadEye)).toVec2f()

                val vertices = arrayOf(posCurrentScreen, posAheadScreen, posAheadEyeScreen)

                glDisable(GL_TEXTURE_2D)
                RenderUtils2D.drawLineStrip(vertices, 2f, ColorRGB(80, 255, 80))
                glEnable(GL_TEXTURE_2D)
            }
        }

        safeListener<RunGameLoopEvent.Tick>(5000) {
            if (timer.tickAndReset(25) && !job.isActiveOrFalse) {
                job = defaultScope.launch { updateTarget() }
            }
        }

        listener<TickEvent.Post> {
            if (isActive() && pauseBaritone.value) {
                pauseBaritone()
                resumeTimer.reset()
                paused = true
            } else if (resumeTimer.tick(resumeDelay.value.toLong())) {
                unpauseBaritone()
                paused = false
            }
        }
    }

    fun getPredictTicksTarget() =
        if (motionPredict.value) getPredictTicks(pingSync.value, ticksAhead)
        else 0

    fun getPredictTicksSelf() =
        if (motionPredict.value && selfPredict.value) getPredictTicks(pingSyncSelf.value, ticksAheadSelf)
        else 0

    private fun getPredictTicks(pingSync: Boolean, ticksAhead: Int) =
        if (pingSync) (InfoCalculator.ping() / 25.0f).fastCeil()
        else ticksAhead

    private fun SafeClientEvent.updateTarget() {
        CombatManager.getTopModule()?.let {
            overrideRange = if (it is KillAura) it.range else targetRange
        }

        val eyePos = player.eyePosition
        val ignoreWall = shouldIgnoreWall()
        val set = getTargetList()

        val wallRangeSq = wallRange.sq
        val newTarget = set.firstOrNull {
            (overrideRange == targetRange || it.distanceTo(eyePos) < overrideRange)
                && (ignoreWall || (player.canEntityBeSeen(it) && player.getDistanceSq(it) <= wallRangeSq))
        }
        val tracker = CombatManager.trackerTarget?.takeIf { it.entity === newTarget }
            ?: newTarget?.let { MotionTracker(it) }

        CombatManager.targetList = set
        CombatManager.target = newTarget
        CombatManager.trackerTarget = tracker
    }

    private fun SafeClientEvent.getTargetList(): Set<EntityLivingBase> {
        val eyePos = player.eyePosition
        val list = ArrayList<Pair<EntityLivingBase, Float>>()

        for (entity in EntityManager.livingBase) {
            if (entity == player || entity == mc.renderViewEntity) continue

            if (!entity.isEntityAlive) continue
            if (!invisible && entity.isInvisible) continue

            val dist = entity.distanceTo(eyePos)
            if (dist > targetRange) continue
            if (!checkEntityType(entity)) continue
            if (AntiBot.isBot(entity)) continue

            val distFactor = if (distancePriority == 0.0f) 0.0f else reverseAndClamp(dist.toFloat() / 8.0f) * distancePriority
            val healthFactor = if (healthPriority == 0.0f) 0.0f else reverseAndClamp(entity.health / 20.0f) * healthPriority
            val armorFactor = if (armorPriority == 0.0f) 0.0f else calcArmorFactor(entity) * armorPriority
            val holeFactor = if (holePriority == 0.0f) 0.0f else calcHoleFactor(entity) * holePriority
            val crosshairFactor = if (crosshairPriority == 0.0f) 0.0f else reverseAndClamp(getRelativeRotation(entity) / 30.0f) * crosshairPriority

            val totalFactor = distFactor + healthFactor + armorFactor + holeFactor + crosshairFactor

            list.add(entity to totalFactor)
        }

        val set = LinkedHashSet<EntityLivingBase>()
        list.sortedByDescending {
            it.second
        }.mapTo(set) {
            it.first
        }

        return set
    }

    private fun calcArmorFactor(entity: EntityLivingBase) =
        CombatManager.getDamageReduction(entity)?.let {
            it.calcDamage(20.0f, TrollAura.isEnabled) / 20.0f
        } ?: 0.0f

    private fun SafeClientEvent.calcHoleFactor(entity: EntityLivingBase): Float {
        if (TrollAura.isDisabled) return 0.0f
        val pos = entity.flooredPosition
        return SurroundUtils.surroundOffsetNoFloor.count {
            !CrystalUtils.isResistant(world.getBlockState(pos.add(it)))
        } / 4.0f
    }

    private fun reverseAndClamp(input: Float) = 1.0f - (input).coerceIn(0.0f, 1.0f)

    private fun checkEntityType(entity: EntityLivingBase): Boolean {
        return if (entity is EntityPlayer) {
            players.value
                && (friends || !FriendManager.isFriend(entity.name))
                && (teammate || !isTeammate(entity))
        } else {
            mobs.value
                && (passive || !entity.isPassive)
                && (neutral || !entity.isNeutral)
                && (hostile || !entity.isHostile)
                && (tamed || !entity.isTamed)
        }
    }

    private fun isTeammate(entity: EntityPlayer): Boolean {
        return runSafeOrFalse {
            val playerTeam = player.team
            val targetTeam = entity.team
            if (playerTeam != null && targetTeam != null && playerTeam.isSameTeam(targetTeam)) {
                return true
            }

            val targetName = entity.displayName.formattedText.replaceFirst("§r", "")
            val clientName = player.displayName.formattedText.replaceFirst("§r", "")

            if (targetName.startsWith("T") && clientName.startsWith("T")) {
                val char1 = targetName[1]
                val char2 = targetName[2]
                if (char1 == char2 && char1.isDigit()) return true
            }

            return targetName.startsWith("§${clientName[1]}")
        }
    }

    private fun shouldIgnoreWall(): Boolean {
        val module = CombatManager.getTopModule()
        return if (module is KillAura) ignoreWalls
        else true
    }
}
