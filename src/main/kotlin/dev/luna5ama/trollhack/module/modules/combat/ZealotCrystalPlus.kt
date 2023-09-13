package dev.luna5ama.trollhack.module.modules.combat

import dev.fastmc.common.*
import dev.fastmc.common.collection.CircularArray
import dev.luna5ama.trollhack.TrollHackMod
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.RunGameLoopEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.events.WorldEvent
import dev.luna5ama.trollhack.event.events.player.OnUpdateWalkingPlayerEvent
import dev.luna5ama.trollhack.event.events.render.Render2DEvent
import dev.luna5ama.trollhack.event.events.render.Render3DEvent
import dev.luna5ama.trollhack.event.events.render.RenderEntityEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.graphics.*
import dev.luna5ama.trollhack.graphics.color.ColorRGB
import dev.luna5ama.trollhack.graphics.color.setGLColor
import dev.luna5ama.trollhack.graphics.font.renderer.MainFontRenderer
import dev.luna5ama.trollhack.gui.hudgui.elements.client.Watermark
import dev.luna5ama.trollhack.manager.managers.*
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager.ghostSwitch
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager.serverSideItem
import dev.luna5ama.trollhack.manager.managers.PlayerPacketManager.sendPlayerPacket
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.module.modules.client.GuiSetting
import dev.luna5ama.trollhack.module.modules.exploit.Bypass
import dev.luna5ama.trollhack.module.modules.player.PacketMine
import dev.luna5ama.trollhack.util.EntityUtils.eyePosition
import dev.luna5ama.trollhack.util.EntityUtils.isPassive
import dev.luna5ama.trollhack.util.MovementUtils.realSpeed
import dev.luna5ama.trollhack.util.SwingMode
import dev.luna5ama.trollhack.util.accessor.*
import dev.luna5ama.trollhack.util.atValue
import dev.luna5ama.trollhack.util.collections.averageOrZero
import dev.luna5ama.trollhack.util.collections.forEachFast
import dev.luna5ama.trollhack.util.collections.none
import dev.luna5ama.trollhack.util.combat.CombatUtils.scaledHealth
import dev.luna5ama.trollhack.util.combat.CombatUtils.totalHealth
import dev.luna5ama.trollhack.util.combat.CrystalDamage
import dev.luna5ama.trollhack.util.combat.CrystalUtils
import dev.luna5ama.trollhack.util.combat.CrystalUtils.canPlaceCrystalOn
import dev.luna5ama.trollhack.util.combat.CrystalUtils.hasValidSpaceForCrystal
import dev.luna5ama.trollhack.util.combat.ExposureSample
import dev.luna5ama.trollhack.util.delegate.CachedValueN
import dev.luna5ama.trollhack.util.interfaces.DisplayEnum
import dev.luna5ama.trollhack.util.inventory.attackDamage
import dev.luna5ama.trollhack.util.inventory.duraPercentage
import dev.luna5ama.trollhack.util.inventory.operation.swapToSlot
import dev.luna5ama.trollhack.util.inventory.slot.*
import dev.luna5ama.trollhack.util.math.RotationUtils
import dev.luna5ama.trollhack.util.math.RotationUtils.getRotationTo
import dev.luna5ama.trollhack.util.math.VectorUtils.toViewVec
import dev.luna5ama.trollhack.util.math.vector.*
import dev.luna5ama.trollhack.util.pause.HandPause
import dev.luna5ama.trollhack.util.pause.MainHandPause
import dev.luna5ama.trollhack.util.pause.withPause
import dev.luna5ama.trollhack.util.threads.*
import dev.luna5ama.trollhack.util.world.*
import it.unimi.dsi.fastutil.ints.Int2LongMaps
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap
import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import it.unimi.dsi.fastutil.longs.Long2LongMaps
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.minecraft.block.state.IBlockState
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.client.renderer.BufferBuilder
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.SharedMonsterAttributes
import net.minecraft.entity.item.EntityEnderCrystal
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Items
import net.minecraft.init.MobEffects
import net.minecraft.init.SoundEvents
import net.minecraft.inventory.EntityEquipmentSlot
import net.minecraft.item.ItemFood
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemSword
import net.minecraft.item.ItemTool
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock
import net.minecraft.network.play.client.CPacketUseEntity
import net.minecraft.network.play.server.SPacketSoundEffect
import net.minecraft.network.play.server.SPacketSpawnObject
import net.minecraft.util.CombatRules
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.EnumHandSide
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.EnumDifficulty
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL20.glUseProgram
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.system.measureNanoTime

@CombatManager.CombatModule
internal object ZealotCrystalPlus : Module(
    name = "Zealot Crystal+",
    description = "Lol wtf",
    category = Category.COMBAT,
    modulePriority = 80
) {
    private val page by setting("Page", Page.GENERAL)

    // General
    private val players by setting("Players", true, { page == Page.GENERAL })
    private val mobs by setting("Mobs", false, { page == Page.GENERAL })
    private val animals by setting("Animals", false, { page == Page.GENERAL })
    private val maxTargets by setting("Max Targets", 4, 1..10, 1, { page == Page.GENERAL })
    private val targetRange by setting("Target Range", 16.0f, 0.0f..32.0f, 1.0f, { page == Page.GENERAL })
    private val yawSpeed by setting("Yaw Speed", 45.0f, 5.0f..180.0f, 5.0f, { page == Page.GENERAL })
    private val placeRotationRange by setting(
        "Place Rotation Range",
        0.0f,
        0.0f..180.0f,
        5.0f,
        { page == Page.GENERAL })
    private val breakRotationRange by setting(
        "Break Rotation Range",
        90.0f,
        0.0f..180.0f,
        5.0f,
        { page == Page.GENERAL })
    private val eatingPause by setting("Eating Pause", false, { page == Page.GENERAL })
    private val updateDelay by setting("Update Delay", 5, 0..250, 1, { page == Page.GENERAL })
    private val globalDelay by setting("Global Delay", 1_000_000, 1_000..10_000_000, 1_000, { page == Page.GENERAL })

    // Force Place
    private val forcePlaceHealth by setting("Force Place Health", 8.0f, 0.0f..20.0f, 0.5f, { page == Page.FORCE_PLACE })
    private val forcePlaceArmorRate by setting("Force Place Armor Rate", 3, 0..25, 1, { page == Page.FORCE_PLACE })
    private val forcePlaceMinDamage by setting(
        "Force Place Min Damage",
        1.5f,
        0.0f..10.0f,
        0.25f,
        { page == Page.FORCE_PLACE })
    private val forcePlaceMotion by setting(
        "Force Place Motion",
        4.0f,
        0.0f..10.0f,
        0.25f,
        { page == Page.FORCE_PLACE })
    private val forcePlaceBalance by setting(
        "Force Place Balance",
        -1.0f,
        -10.0f..10.0f,
        0.25f,
        { page == Page.FORCE_PLACE })
    private val forcePlaceWhileSwording by setting("Force Place While Swording", false, { page == Page.FORCE_PLACE })

    // Calculation
    private val assumeInstantMine by setting("Assume Instant Mine", true, { page == Page.CALCULATION })
    private val noSuicide by setting("No Suicide", 2.0f, 0.0f..20.0f, 0.25f, { page == Page.CALCULATION })
    private val wallRange by setting("Wall Range", 3.0f, 0.0f..8.0f, 0.1f, { page == Page.CALCULATION })
    private val motionPredict by setting("Motion Predict", true, { page == Page.CALCULATION })
    private val predictTicks by setting("Predict Ticks", 8, 0..20, 1, { page == Page.CALCULATION && motionPredict })
    private val damagePriority by setting("Damage Priority", DamagePriority.EFFICIENT, ::page.atValue(Page.CALCULATION))
    private val lethalOverride by setting("Lethal Override", true, { page == Page.CALCULATION })
    private val lethalThresholdAddition by setting(
        "Lethal Threshole Addition",
        0.5f,
        -5.0f..5.0f,
        0.1f,
        { page == Page.CALCULATION && lethalOverride })
    private val lethalMaxSelfDamage by setting(
        "Lethal Max Self Damage",
        16.0f,
        0.0f..20.0f,
        0.25f,
        { page == Page.CALCULATION && lethalOverride })
    private val safeMaxTargetDamageReduction by setting(
        "Safe Max Target Damage Reduction",
        1.0f,
        0.0f..10.0f,
        0.1f,
        { page == Page.CALCULATION })
    private val safeMinSelfDamageReduction by setting(
        "Safe Min Self Damage Reduction",
        2.0f,
        0.0f..10.0f,
        0.1f,
        { page == Page.CALCULATION })
    private val collidingCrystalExtraSelfDamageThreshold by setting(
        "Colliiding Crystal Extra Self Damage Threshold",
        4.0f,
        0.0f..10.0f,
        0.1f,
        { page == Page.CALCULATION })

    // Place
    private val placeMode by setting("Place Mode", PlaceMode.SINGLE, { page == Page.PLACE })
    private val packetPlace by setting("Packet Place", PacketPlaceMode.WEAK, { page == Page.PLACE })
    private val spamPlace by setting("Spam Place", false, { page == Page.PLACE })
    private val placeSwitchMode by setting("Place Switch Mode", SwitchMode.OFF, { page == Page.PLACE })
    private val placeSwitchBypass by setting(
        "Place Switch Bypass",
        HotbarSwitchManager.Override.PICK,
        { page == Page.PLACE && placeSwitchMode == SwitchMode.GHOST })
    private val placeSwing by setting("Place Swing", false, { page == Page.PLACE })
    private val placeSideBypass by setting("Place Side Bypass", PlaceBypass.UP, { page == Page.PLACE })
    private val placeMinDamage by setting("Place Min Damage", 5.0f, 0.0f..20.0f, 0.25f, { page == Page.PLACE })
    private val placeMaxSelfDamage by setting("Place Max Self Damage", 6.0f, 0.0f..20.0f, 0.25f, { page == Page.PLACE })
    private val placeBalance by setting("Place Balance", -3.0f, -10.0f..10.0f, 0.25f, { page == Page.PLACE })
    private val placeDelay by setting("Place Delay", 50, 0..500, 1, { page == Page.PLACE })
    private val placeRange by setting("Place Range", 5.0f, 0.0f..8.0f, 0.1f, { page == Page.PLACE })
    private val placeRangeMode by setting("Place Range Mode", RangeMode.FEET, { page == Page.PLACE })

    // Break
    private val breakMode by setting("Break Mode", BreakMode.SMART, { page == Page.BREAK })
    private val bbtt by setting("2B2T", false, { page == Page.BREAK })
    private val bbttFactor by setting("2B2T Factor", 200, 0..1000, 25, { page == Page.BREAK && bbtt })
    private val packetBreak by setting("Packet Break", BreakMode.TARGET, { page == Page.BREAK && !bbtt })
    private val ownTimeout by setting(
        "Own Timeout",
        100,
        0..2000,
        25,
        { page == Page.BREAK && (breakMode == BreakMode.OWN || packetBreak == BreakMode.OWN) })
    private val antiWeakness by setting("Anti Weakness", SwitchMode.OFF, { page == Page.BREAK })
    private val antiWeaknessBypass by setting(
        "Anti Weakness Byass",
        HotbarSwitchManager.Override.DEFAULT,
        { page == Page.BREAK && antiWeakness == SwitchMode.GHOST })
    private val swapDelay by setting("Swap Delay", 0, 0..20, 1, { page == Page.BREAK })
    private val breakMinDamage by setting("Break Min Damage", 4.0f, 0.0f..20.0f, 0.25f, { page == Page.BREAK })
    private val breakMaxSelfDamage by setting("Break Max Self Damage", 8.0f, 0.0f..20.0f, 0.25f, { page == Page.BREAK })
    private val breakBalance by setting("Break Balance", -4.0f, -10.0f..10.0f, 0.25f, { page == Page.BREAK })
    private val breakDelay by setting("Break Delay", 100, 0..500, 1, { page == Page.BREAK })
    private val breakRange by setting("Break Range", 5.0f, 0.0f..8.0f, 0.1f, { page == Page.BREAK })
    private val breakRangeMode by setting("Break Range Mode", RangeMode.FEET, { page == Page.BREAK })

    // Misc
    private val swingMode by setting("Swing Mode", SwingMode.CLIENT, { page == Page.RENDER })
    private val swingHand by setting("Swing Hand", SwingHand.AUTO, { page == Page.RENDER })
    private val filledAlpha by setting("Filled Alpha", 63, 0..255, 1, { page == Page.RENDER })
    private val outlineAlpha by setting("Outline Alpha", 200, 0..255, 1, { page == Page.RENDER })
    private val targetDamage by setting("Target Damage", true, { page == Page.RENDER })
    private val selfDamage by setting("Self Damage", true, { page == Page.RENDER })
    private val targetChams by setting("Target Chams", RenderMode.SINGLE, { page == Page.RENDER })
    private val chamsAlpha by setting(
        "Chams Alpha",
        64,
        0..255,
        1,
        { page == Page.RENDER && targetChams != RenderMode.OFF })
    private val renderPredict by setting("Render Predict", RenderMode.SINGLE, { page == Page.RENDER })
    private val hudInfo by setting("Hud Info", HudInfo.SPEED, { page == Page.RENDER })
    private val movingLength by setting("Moving Length", 400, 0..1000, 50, { page == Page.RENDER })
    private val fadeLength by setting("Fade Length", 200, 0..1000, 50, { page == Page.RENDER })

    private enum class Page(override val displayName: String) : DisplayEnum {
        GENERAL("General"),
        FORCE_PLACE("Force Place"),
        CALCULATION("Calculation"),
        PLACE("Place"),
        BREAK("Break"),
        RENDER("Render")
    }

    private enum class DamagePriority(override val displayName: String) : DisplayEnum {
        EFFICIENT("Efficient") {
            override operator fun invoke(selfDamage: Float, targetDamage: Float): Float {
                return targetDamage - selfDamage
            }
        },
        AGGRESSIVE("Aggressive") {
            override operator fun invoke(selfDamage: Float, targetDamage: Float): Float {
                return targetDamage
            }
        };

        abstract operator fun invoke(selfDamage: Float, targetDamage: Float): Float
    }

    private enum class SwingHand(override val displayName: String) : DisplayEnum {
        AUTO("Auto"),
        OFF_HAND("Off Hand"),
        MAIN_HAND("Main Hand")
    }

    private enum class SwitchMode(override val displayName: String) : DisplayEnum {
        OFF("Off"),
        LEGIT("Legit"),
        GHOST("Ghost")
    }

    @Suppress("unused")
    private enum class PlaceMode(override val displayName: String) : DisplayEnum {
        OFF("Off"),
        SINGLE("Single"),
        MULTI("Multi")
    }

    @Suppress("unused")
    private enum class PacketPlaceMode(override val displayName: String, val onRemove: Boolean, val onBreak: Boolean) :
        DisplayEnum {
        OFF("Off", false, false),
        WEAK("Weak", true, false),
        STRONG("Strong", true, true)
    }

    private enum class PlaceBypass(override val displayName: String) : DisplayEnum {
        UP("Up"),
        DOWN("Down"),
        CLOSEST("Closest")
    }

    private enum class BreakMode(override val displayName: String) : DisplayEnum {
        OFF("Off"),
        TARGET("Target"),
        OWN("Own"),
        SMART("Smart"),
        ALL("All"),
    }

    private enum class RangeMode(override val displayName: String) : DisplayEnum {
        FEET("Feet"),
        EYES("Eye")
    }

    private enum class RenderMode(override val displayName: String) : DisplayEnum {
        OFF("Off"),
        SINGLE("Single"),
        MULTI("Multi")
    }

    private enum class HudInfo(override val displayName: String) : DisplayEnum {
        OFF("Off"),
        SPEED("Speed"),
        TARGET("Target"),
        DAMAGE("Damage"),
        CALCULATION_TIME("Calculation Time")
    }

    override fun isActive(): Boolean {
        return isEnabled && target != null && !paused()
    }

    private val renderTargetSet = CachedValueN(5L) {
        IntOpenHashSet().apply {
            targets.getLazy()?.forEach {
                add(it.entity.entityId)
            }
        }
    }

    private val targets = CachedValueN(25L) {
        runSafe {
            getTargets()
        } ?: emptySequence()
    }

    private val rawPosList = CachedValueN(60L) {
        runSafe {
            getRawPosList()
        } ?: emptyList()
    }

    private val rotationInfo = CachedValueN(25L, PlaceInfo.INVALID) {
        runSafe {
            calcPlaceInfo(false)
        }
    }

    private val placeInfo = CachedValueN(25L, PlaceInfo.INVALID) {
        runSafe {
            calcPlaceInfo(Bypass.crystalRotation)
        }
    }

    private val renderPlaceInfo: PlaceInfo?
        get() = if (Bypass.crystalRotation) rotationInfo.getLazy() else placeInfo.getLazy()

    @JvmStatic
    val target: EntityLivingBase?
        get() = placeInfo.getLazy()?.target

    private val placedPosMap = Long2LongMaps.synchronize(Long2LongOpenHashMap())
    private val crystalSpawnMap = Int2LongMaps.synchronize(Int2LongOpenHashMap())
    private val attackedCrystalMap = Int2LongMaps.synchronize(Int2LongOpenHashMap())
    private val attackedPosMap = Long2LongMaps.synchronize(Long2LongOpenHashMap())

    private val timeoutTimer = TickTimer()
    private val placeTimer = TickTimer()
    private val breakTimer = TickTimer()

    var lastActiveTime = 0L; private set
    private var lastRotation: PlaceInfo? = null

    private val explosionTimer = TickTimer()
    private val explosionCountArray = CircularArray<Int>(8)
    private var explosionCount = 0

    private val calculationTimes = CircularArray<Int>(100)
    private var calculationTimesPending = IntArrayList()

    private val loopThread = Thread({
        var updateTask: Job? = null
        var loopTask: Job? = null

        while (true) {
            val loopStart = System.nanoTime()
            try {
                while (isDisabled || mc.world == null) {
                    try {
                        Thread.sleep(1000L)
                    } catch (e: InterruptedException) {
                        break
                    }
                }

                if (!updateTask.isActiveOrFalse) {
                    updateTask = ConcurrentScope.launch {
                        try {
                            targets.get()
                            if (Bypass.crystalRotation) rotationInfo.get(mc.timer.tickLength.toInt())
                            placeInfo.get(updateDelay)

                            if (explosionTimer.tickAndReset(250L)) {
                                val count = explosionCount
                                explosionCount = 0
                                explosionCountArray.add(count)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                if (!loopTask.isActiveOrFalse) {
                    loopTask = ConcurrentScope.launch {
                        try {
                            runLoop()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                try {
                    val interval = globalDelay - (System.nanoTime() - loopStart)
                    if (interval > 0) Thread.sleep(interval / 1_000_000, (interval % 1_000_000).toInt())
                } catch (_: InterruptedException) {

                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }, "Zealot+ Loop").apply {
        isDaemon = true
        start()
    }

    init {
        onEnable {
            loopThread.interrupt()
            TrollHackMod.logger.trace(Watermark.nameAsString)
        }

        onDisable {
            placeTimer.reset(-114514L)
            breakTimer.reset(-114514L)

            lastActiveTime = 0L
            lastRotation = null

            explosionTimer.reset(-114514L)
            explosionCountArray.clear()
            explosionCount = 0

            calculationTimes.clear()

            Renderer.reset()
        }
    }

    override fun getHudInfo(): String {
        return when (hudInfo) {
            HudInfo.OFF -> ""
            HudInfo.SPEED -> "%.1f".format(explosionCountArray.averageOrZero() * 4.0)
            HudInfo.DAMAGE -> renderPlaceInfo?.let { "%.1f/%.1f".format(it.targetDamage, it.selfDamage) }
                ?: "0.0/0.0"
            HudInfo.TARGET -> target?.name ?: "None"
            HudInfo.CALCULATION_TIME -> "%.2f ms".format(calculationTimes.averageOrZero() / 1_000_000.0)
        }
    }

    init {
        safeListener<Render3DEvent> {
            if (renderPredict != RenderMode.OFF) {
                val tessellator = Tessellator.getInstance()
                val buffer = tessellator.buffer
                val partialTicks = RenderUtils3D.partialTicks

                GlStateManager.color(0.3f, 1.0f, 0.3f, 1.0f)
                GlStateManager.glLineWidth(2.0f)

                buffer.begin(GL_LINES, DefaultVertexFormats.POSITION)
                buffer.setTranslation(
                    -mc.renderManager.renderPosX,
                    -mc.renderManager.renderPosY,
                    -mc.renderManager.renderPosZ
                )

                if (renderPredict == RenderMode.SINGLE) {
                    val placeInfo = renderPlaceInfo
                    if (placeInfo != null) {
                        targets.getLazy()?.find {
                            it.entity == placeInfo.target
                        }?.let {
                            drawEntityPrediction(buffer, it.entity, it.predictMotion, partialTicks)
                        }
                    } else {
                        targets
                    }
                } else {
                    targets.getLazy()?.forEach {
                        drawEntityPrediction(buffer, it.entity, it.predictMotion, partialTicks)
                    }
                }

                glUseProgram(0)
                tessellator.draw()
                buffer.setTranslation(0.0, 0.0, 0.0)

                GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)
                GlStateManager.glLineWidth(1.0f)
            }

            Renderer.onRender3D()
        }

        safeListener<Render2DEvent.Absolute> {
            Renderer.onRender2D()
        }

        safeListener<RenderEntityEvent.Model.Pre> {
            if (!it.cancelled && isValidEntityForRendering(targetChams, it.entity)) {
                glDepthRange(0.0, 0.01)
                GuiSetting.primary.alpha(chamsAlpha).setGLColor()
                GlStateManager.disableTexture2D()
                GlStateManager.disableLighting()
                GlStateManager.enableBlend()
                GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO)
            }
        }

        safeListener<RenderEntityEvent.Model.Post> {
            if (!it.cancelled && isValidEntityForRendering(targetChams, it.entity)) {
                GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)
                GlStateManager.enableTexture2D()
                GlStateManager.enableLighting()
            }
        }

        safeListener<RenderEntityEvent.All.Post> {
            if (!it.cancelled && isValidEntityForRendering(targetChams, it.entity)) {
                glDepthRange(0.0, 1.0)
            }
        }

        safeListener<OnUpdateWalkingPlayerEvent.Pre>(114514) {
            if (paused()) return@safeListener

            if (!Bypass.crystalRotation) return@safeListener

            var placing = System.currentTimeMillis() - lastActiveTime <= 250L
            rotationInfo.get(mc.timer.tickLength.toInt() * 2)?.let {
                lastRotation = it
                placing = true
            }

            if (placing) {
                lastRotation?.let {
                    val rotation = getRotationTo(it.hitVec)
                    val diff = RotationUtils.calcAngleDiff(rotation.x, PlayerPacketManager.rotation.x)

                    if (abs(diff) <= yawSpeed) {
                        sendPlayerPacket {
                            rotate(rotation)
                        }
                    } else {
                        val clamped = diff.coerceIn(-yawSpeed, yawSpeed)
                        val newYaw = RotationUtils.normalizeAngle(PlayerPacketManager.rotation.x + clamped)

                        sendPlayerPacket {
                            rotate(Vec2f(newYaw, rotation.y))
                        }
                    }
                }
            } else {
                lastRotation = null
            }
        }

        safeParallelListener<TickEvent.Post> {
            for (entity in EntityManager.entity) {
                if (entity !is EntityLivingBase) continue
                reductionMap[entity] = DamageReduction(entity)
            }

            rawPosList.updateForce()
        }

        safeListener<WorldEvent.ClientBlockUpdate>(114514) {
            if (player.distanceSqToCenter(it.pos) < (placeRange.ceilToInt() + 1).sq
                && checkResistant(it.pos, it.oldState) != checkResistant(it.pos, it.newState)
            ) {
                rawPosList.updateLazy()
                rotationInfo.updateLazy()
                placeInfo.updateLazy()
            }
        }

        safeListener<RunGameLoopEvent.Render>(114514) {
            val list: IntArrayList
            synchronized(calculationTimes) {
                list = calculationTimesPending
                calculationTimesPending = IntArrayList()
            }
            calculationTimes.addAll(list)
        }
    }

    private fun isValidEntityForRendering(renderMode: RenderMode, entity: Entity): Boolean {
        return when (renderMode) {
            RenderMode.OFF -> false
            RenderMode.SINGLE -> entity == (target ?: targets.getLazy()?.firstOrNull())
            RenderMode.MULTI -> renderTargetSet.get().contains(entity.entityId)
        }
    }

    private fun drawEntityPrediction(buffer: BufferBuilder, entity: Entity, motion: Vec3d, partialTicks: Float) {
        val x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks
        val y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks
        val z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks

        val endX = x + motion.x
        val endY = y + motion.y
        val endZ = z + motion.z

        buffer.pos(x, y, z).endVertex()
        buffer.pos(endX, endY, endZ).endVertex()
        buffer.pos(endX, endY, endZ).endVertex()
        buffer.pos(endX, endY + entity.eyeHeight, endZ).endVertex()
    }

    @JvmStatic
    fun handleSpawnPacket(packet: SPacketSpawnObject) {
        if (isDisabled || packet.type != 51) return

        runSafe {
            val mutableBlockPos = BlockPos.MutableBlockPos()
            if (checkBreakRange(packet.x, packet.y, packet.z, mutableBlockPos)) {
                if (!paused() && !bbtt && checkCrystalRotation(packet.x, packet.y, packet.z)) {
                    placeInfo.getLazy()?.let {
                        when (packetBreak) {
                            BreakMode.TARGET -> {
                                if (CrystalUtils.crystalPlaceBoxIntersectsCrystalBox(
                                        it.blockPos,
                                        packet.x,
                                        packet.y,
                                        packet.z
                                    )
                                ) {
                                    breakDirect(packet.x, packet.y, packet.z, packet.entityID)
                                }
                            }
                            BreakMode.OWN -> {
                                if (CrystalUtils.crystalPlaceBoxIntersectsCrystalBox(
                                        it.blockPos,
                                        packet.x,
                                        packet.y,
                                        packet.z
                                    )
                                    || placedPosMap.containsKey(toLong(packet.x, packet.y - 1.0, packet.z))
                                    && checkBreakDamage(packet.x, packet.y, packet.z, mutableBlockPos)
                                ) {
                                    breakDirect(packet.x, packet.y, packet.z, packet.entityID)
                                }
                            }
                            BreakMode.SMART -> {
                                if (CrystalUtils.crystalPlaceBoxIntersectsCrystalBox(
                                        it.blockPos,
                                        packet.x,
                                        packet.y,
                                        packet.z
                                    )
                                    || checkBreakDamage(packet.x, packet.y, packet.z, mutableBlockPos)
                                ) {
                                    breakDirect(packet.x, packet.y, packet.z, packet.entityID)
                                }
                            }
                            BreakMode.ALL -> {
                                breakDirect(packet.x, packet.y, packet.z, packet.entityID)
                            }
                            else -> {
                                return
                            }
                        }
                    }
                }

                crystalSpawnMap[packet.entityID] = System.currentTimeMillis()
            }
        }
    }

    private fun SafeClientEvent.checkBreakDamage(
        crystalX: Double,
        crystalY: Double,
        crystalZ: Double,
        mutableBlockPos: BlockPos.MutableBlockPos
    ): Boolean {
        val context = CombatManager.contextSelf ?: return false
        val selfDamage = max(
            context.calcDamage(crystalX, crystalY, crystalZ, false, mutableBlockPos),
            context.calcDamage(crystalX, crystalY, crystalZ, true, mutableBlockPos)
        )
        if (player.scaledHealth - selfDamage <= noSuicide) return false

        return targets.get(100L).any {
            checkBreakDamage(crystalX, crystalY, crystalZ, selfDamage, it, mutableBlockPos)
        }
    }

    private fun SafeClientEvent.checkBreakDamage(
        crystalX: Double,
        crystalY: Double,
        crystalZ: Double,
        selfDamage: Float,
        targetInfo: TargetInfo,
        mutableBlockPos: BlockPos.MutableBlockPos
    ): Boolean {
        val targetDamage =
            calcDamage(targetInfo.entity, targetInfo.pos, targetInfo.box, crystalX, crystalY, crystalZ, mutableBlockPos)
        if (lethalOverride && targetDamage - targetInfo.entity.totalHealth > lethalThresholdAddition && targetDamage <= lethalMaxSelfDamage) {
            return true
        }

        if (selfDamage > breakMaxSelfDamage) return false

        val minDamage: Float
        val balance: Float

        if (shouldForcePlace(targetInfo.entity)) {
            minDamage = forcePlaceMinDamage
            balance = forcePlaceBalance
        } else {
            minDamage = breakMinDamage
            balance = breakBalance
        }

        return targetDamage >= minDamage && targetDamage - selfDamage >= balance
    }

    @JvmStatic
    fun handleExplosion(packet: SPacketSoundEffect) {
        if (isDisabled || packet.sound != SoundEvents.ENTITY_GENERIC_EXPLODE) return

        runSafe {
            val placeInfo = placeInfo.getLazy()
            if (placeInfo != null) {
                if (distanceSq(
                        placeInfo.blockPos.x + 0.5, placeInfo.blockPos.y + 1.0, placeInfo.blockPos.z + 0.5,
                        packet.x, packet.y, packet.z
                    ) <= 144.0
                ) {
                    placedPosMap.clear()

                    if (packetPlace.onRemove) {
                        placeDirect(placeInfo)
                    }

                    if (attackedPosMap.containsKey(
                            toLong(
                                packet.x.floorToInt(),
                                packet.y.floorToInt(),
                                packet.z.floorToInt()
                            )
                        )
                    ) {
                        explosionCount++
                    }

                    crystalSpawnMap.clear()
                    attackedCrystalMap.clear()
                    attackedPosMap.clear()
                }
            } else if (player.distanceSqTo(packet.x, packet.y, packet.z) <= 144.0) {
                placedPosMap.clear()
                crystalSpawnMap.clear()
                attackedCrystalMap.clear()
                attackedPosMap.clear()
            }
        }
    }


    private fun paused(): Boolean {
        return AutoCity.isActive()
    }

    private fun runLoop() {
        if (paused()) return

        val breakFlag = breakMode != BreakMode.OFF && breakTimer.tick(breakDelay)
        val placeFlag = placeMode != PlaceMode.OFF && placeTimer.tick(placeDelay)

        if (timeoutTimer.tickAndReset(5L)) {
            updateTimeouts()
        }

        if (breakFlag || placeFlag) {
            runSafe {
                val placeInfo = placeInfo.get(updateDelay)
                placeInfo?.let {
                    if (checkPausing()) return
                    if (breakFlag) doBreak(placeInfo)
                    if (placeFlag) doPlace(placeInfo)
                }
            }
        }
    }

    private fun updateTimeouts() {
        val current = System.currentTimeMillis()

        placedPosMap.runSynchronized {
            values.removeIf {
                it < current
            }
        }

        crystalSpawnMap.runSynchronized {
            values.removeIf {
                it + 5000L < current
            }
        }

        attackedCrystalMap.runSynchronized {
            values.removeIf {
                it < current
            }
        }

        attackedPosMap.runSynchronized {
            values.removeIf {
                it < current
            }
        }
    }

    private fun SafeClientEvent.checkPausing(): Boolean {
        return eatingPause && player.isHandActive && player.activeItemStack.item is ItemFood
    }

    private fun SafeClientEvent.doBreak(placeInfo: PlaceInfo) {
        val crystalList = getCrystalList()

        val crystal = when (breakMode) {
            BreakMode.OWN -> {
                getTargetCrystal(placeInfo, crystalList)
                    ?: getCrystal(crystalList.filter {
                        placedPosMap.containsKey(
                            toLong(
                                it.posX,
                                it.posY - 1.0,
                                it.posZ
                            )
                        )
                    })
            }
            BreakMode.TARGET -> {
                getTargetCrystal(placeInfo, crystalList)
            }
            BreakMode.SMART -> {
                getTargetCrystal(placeInfo, crystalList)
                    ?: getCrystal(crystalList)
            }
            BreakMode.ALL -> {
                val entity = target ?: player
                crystalList.minByOrNull { entity.distanceSqTo(it) }
            }
            else -> {
                return
            }
        }

        crystal?.let {
            breakDirect(it.posX, it.posY, it.posZ, it.entityId)
        }
    }

    private fun SafeClientEvent.getCrystalList(): List<EntityEnderCrystal> {
        val eyePos = PlayerPacketManager.position.add(0.0, player.getEyeHeight().toDouble(), 0.0)
        val sight = eyePos.add(PlayerPacketManager.rotation.toViewVec().scale(8.0))
        val mutableBlockPos = BlockPos.MutableBlockPos()

        return EntityManager.entity.asSequence()
            .filterIsInstance<EntityEnderCrystal>()
            .filter { it.isEntityAlive }
            .runIf(bbtt) {
                val current = System.currentTimeMillis()
                filter { current - getSpawnTime(it) >= bbttFactor }
            }
            .filter { checkBreakRange(it, mutableBlockPos) }
            .filter { checkCrystalRotation(it.entityBoundingBox, eyePos, sight) }
            .toList()
    }

    private inline fun <T> T.runIf(boolean: Boolean, block: T.() -> T): T {
        return if (boolean) block.invoke(this)
        else this
    }

    private fun getSpawnTime(crystal: EntityEnderCrystal): Long {
        return crystalSpawnMap.computeIfAbsent(crystal.entityId) {
            System.currentTimeMillis() - crystal.ticksExisted * 50
        }
    }

    private fun getTargetCrystal(placeInfo: PlaceInfo, crystalList: List<EntityEnderCrystal>): EntityEnderCrystal? {
        return crystalList.firstOrNull {
            CrystalUtils.crystalPlaceBoxIntersectsCrystalBox(placeInfo.blockPos, it.posX, it.posY, it.posZ)
        }
    }

    @Suppress("DuplicatedCode")
    private fun SafeClientEvent.getCrystal(crystalList: List<EntityEnderCrystal>): EntityEnderCrystal? {
        val max = BreakInfo.Mutable()
        val safe = BreakInfo.Mutable()
        val lethal = BreakInfo.Mutable()

        val targets = targets.get().toList()

        val noSuicide = noSuicide
        val mutableBlockPos = BlockPos.MutableBlockPos()
        val context = CombatManager.contextSelf ?: return null
        val damagePriority = damagePriority

        if (targets.isNotEmpty()) {
            for (crystal in crystalList) {
                val selfDamage = max(
                    context.calcDamage(crystal.posX, crystal.posY, crystal.posZ, false, mutableBlockPos),
                    context.calcDamage(crystal.posX, crystal.posY, crystal.posZ, true, mutableBlockPos)
                )
                if (player.scaledHealth - selfDamage <= noSuicide) continue
                if (!lethalOverride && selfDamage > breakMaxSelfDamage) continue

                for ((entity, entityPos, entityBox) in targets) {
                    val targetDamage = calcDamage(
                        entity,
                        entityPos,
                        entityBox,
                        crystal.posX,
                        crystal.posY,
                        crystal.posZ,
                        mutableBlockPos
                    )
                    if (lethalOverride && System.currentTimeMillis() - CombatManager.getHurtTime(entity) > 400L
                        && targetDamage - entity.totalHealth > lethalThresholdAddition && selfDamage < lethal.selfDamage
                        && selfDamage <= lethalMaxSelfDamage
                    ) {
                        lethal.update(crystal, selfDamage, targetDamage)
                    }

                    if (selfDamage > breakMaxSelfDamage) continue

                    val minDamage: Float
                    val balance: Float

                    if (shouldForcePlace(entity)) {
                        minDamage = forcePlaceMinDamage
                        balance = forcePlaceBalance
                    } else {
                        minDamage = breakMinDamage
                        balance = breakBalance
                    }

                    if (targetDamage < minDamage || targetDamage - selfDamage < balance) continue

                    if (damagePriority(selfDamage, targetDamage) > damagePriority(max.selfDamage, max.targetDamage)) {
                        max.update(crystal, selfDamage, targetDamage)
                    } else if (max.targetDamage - targetDamage <= safeMaxTargetDamageReduction
                        && max.selfDamage - selfDamage >= safeMinSelfDamageReduction
                    ) {
                        safe.update(crystal, selfDamage, targetDamage)
                    }
                }
            }
        }

        if (max.targetDamage - safe.targetDamage > safeMaxTargetDamageReduction
            || max.selfDamage - safe.selfDamage <= safeMinSelfDamageReduction
        ) {
            safe.clear()
        }

        val valid = lethal.takeValid()
            ?: safe.takeValid()
            ?: max.takeValid()

        return valid?.crystal
    }

    private fun SafeClientEvent.checkCrystalRotation(x: Double, y: Double, z: Double): Boolean {
        if (!Bypass.crystalRotation) return true

        val eyePos = PlayerPacketManager.position.add(0.0, player.getEyeHeight().toDouble(), 0.0)
        val sight = eyePos.add(PlayerPacketManager.rotation.toViewVec().scale(8.0))

        return checkCrystalRotation(CrystalUtils.getCrystalBB(x, y, z), eyePos, sight)
    }

    private fun checkCrystalRotation(box: AxisAlignedBB, eyePos: Vec3d, sight: Vec3d): Boolean {
        return !Bypass.crystalRotation
            || box.calculateIntercept(eyePos, sight) != null
            || breakRotationRange != 0.0f && checkRotationDiff(getRotationTo(eyePos, box.center), breakRotationRange)
    }

    private fun SafeClientEvent.doPlace(placeInfo: PlaceInfo) {
        if (spamPlace || checkPlaceCollision(placeInfo)) {
            placeDirect(placeInfo)
        }
    }

    private fun checkPlaceCollision(placeInfo: PlaceInfo): Boolean {
        return EntityManager.entity.asSequence()
            .filterIsInstance<EntityEnderCrystal>()
            .filter { it.isEntityAlive }
            .filter { CrystalUtils.crystalPlaceBoxIntersectsCrystalBox(placeInfo.blockPos, it) }
            .filterNot { attackedCrystalMap.containsKey(it.entityId) }
            .none()
    }


    private fun SafeClientEvent.placeDirect(placeInfo: PlaceInfo) {
        if (player.allSlots.countItem(Items.END_CRYSTAL) == 0) return

        val hand = getHandNullable()

        if (hand == null) {
            when (placeSwitchMode) {
                SwitchMode.OFF -> {
                    return
                }
                SwitchMode.LEGIT -> {
                    val packet = placePacket(placeInfo, EnumHand.MAIN_HAND)
                    synchronized(InventoryTaskManager) {
                        val slot = player.getCrystalSlot() ?: return
                        MainHandPause.withPause(ZealotCrystalPlus, placeDelay * 2) {
                            swapToSlot(slot)
                            connection.sendPacket(packet)
                        }
                    }
                }
                SwitchMode.GHOST -> {
                    val packet = placePacket(placeInfo, EnumHand.MAIN_HAND)
                    synchronized(InventoryTaskManager) {
                        val slot = player.getMaxCrystalSlot() ?: return
                        ghostSwitch(placeSwitchBypass, slot) {
                            connection.sendPacket(packet)
                        }
                    }

                }
            }
        } else {
            synchronized(InventoryTaskManager) {
                HandPause[hand].withPause(ZealotCrystalPlus, placeDelay * 2) {
                    playerController.syncCurrentPlayItem()
                    connection.sendPacket(placePacket(placeInfo, hand))
                }
            }
        }

        placedPosMap[placeInfo.blockPos.toLong()] = System.currentTimeMillis() + ownTimeout
        placeTimer.reset()
        lastActiveTime = System.currentTimeMillis()

        if (placeSwing) {
            onMainThread {
                swingHand()
            }
        }
    }

    private fun placePacket(placeInfo: PlaceInfo, hand: EnumHand): CPacketPlayerTryUseItemOnBlock {
        return CPacketPlayerTryUseItemOnBlock(
            placeInfo.blockPos,
            placeInfo.side,
            hand,
            placeInfo.hitVecOffset.x,
            placeInfo.hitVecOffset.y,
            placeInfo.hitVecOffset.z
        )
    }

    private fun SafeClientEvent.breakDirect(x: Double, y: Double, z: Double, entityID: Int) {
        if (placeSwitchMode != SwitchMode.GHOST
            && antiWeakness != SwitchMode.GHOST
            && System.currentTimeMillis() - HotbarSwitchManager.swapTime < swapDelay * 50L
        ) return

        if (player.isWeaknessActive() && !isHoldingTool()) {
            when (antiWeakness) {
                SwitchMode.OFF -> {
                    return
                }
                SwitchMode.LEGIT -> {
                    val slot = getWeaponSlot() ?: return
                    MainHandPause.withPause(ZealotCrystalPlus, placeDelay * 2) {
                        swapToSlot(slot)
                        if (placeSwitchMode != SwitchMode.GHOST && swapDelay != 0) return@withPause
                        connection.sendPacket(attackPacket(entityID))
                        swingHand()
                    }
                }
                SwitchMode.GHOST -> {
                    val slot = getWeaponSlot() ?: return
                    val packet = attackPacket(entityID)
                    ghostSwitch(antiWeaknessBypass, slot) {
                        connection.sendPacket(packet)
                        swingHand()
                    }
                }
            }
        } else {
            connection.sendPacket(attackPacket(entityID))
            swingHand()
        }

        attackedCrystalMap[entityID] = System.currentTimeMillis() + 1000L
        attackedPosMap[toLong(x.floorToInt(), y.floorToInt(), z.floorToInt())] = System.currentTimeMillis() + 1000L
        breakTimer.reset()

        lastActiveTime = System.currentTimeMillis()

        placeInfo.get(500L)?.let {
            player.setLastAttackedEntity(it.target)
            if (packetPlace.onBreak && CrystalUtils.crystalPlaceBoxIntersectsCrystalBox(it.blockPos, x, y, z)) {
                placeDirect(it)
            }
        }
    }

    private fun attackPacket(entityID: Int): CPacketUseEntity {
        val packet = CPacketUseEntity()
        packet.packetAction = CPacketUseEntity.Action.ATTACK
        packet.id = entityID
        return packet
    }

    private fun EntityPlayerSP.isWeaknessActive(): Boolean {
        return this.isPotionActive(MobEffects.WEAKNESS)
            && this.getActivePotionEffect(MobEffects.STRENGTH)?.let {
            it.amplifier <= 0
        } ?: true
    }

    private fun SafeClientEvent.isHoldingTool(): Boolean {
        val item = player.serverSideItem.item
        return item is ItemTool || item is ItemSword
    }

    private fun EntityPlayerSP.getMaxCrystalSlot(): HotbarSlot? {
        return this.hotbarSlots.asSequence().filter {
            it.stack.item == Items.END_CRYSTAL
        }.maxByOrNull {
            it.stack.count
        }
    }

    private fun EntityPlayerSP.getCrystalSlot(): HotbarSlot? {
        return this.hotbarSlots.firstItem(Items.END_CRYSTAL)
    }

    private fun SafeClientEvent.getWeaponSlot(): HotbarSlot? {
        return player.hotbarSlots.filterByStack {
            val item = it.item
            item is ItemSword || item is ItemTool
        }.maxByOrNull {
            val itemStack = it.stack
            itemStack.attackDamage
        }
    }

    @Suppress("DuplicatedCode")
    private fun SafeClientEvent.calcPlaceInfo(checkRotation: Boolean): PlaceInfo? {
        var placeInfo: PlaceInfo.Mutable? = null
        val time = measureNanoTime {
            val targets = targets.get().toList()
            if (targets.isEmpty()) return@measureNanoTime

            val context = CombatManager.contextSelf ?: return@measureNanoTime

            val mutableBlockPos = BlockPos.MutableBlockPos()
            val targetBlocks = getPlaceablePos(checkRotation, mutableBlockPos)
            if (targetBlocks.isEmpty()) return@measureNanoTime

            val max = PlaceInfo.Mutable(player)
            val safe = PlaceInfo.Mutable(player)
            val lethal = PlaceInfo.Mutable(player)

            val noSuicide = noSuicide
            val crystals = CombatManager.crystalList

            val damagePriority = damagePriority

            for (pos in targetBlocks) {
                val placeBox = CrystalUtils.getCrystalPlacingBB(pos)

                val crystalX = pos.x + 0.5
                val crystalY = pos.y + 1.0
                val crystalZ = pos.z + 0.5

                val selfDamage = max(
                    context.calcDamage(crystalX, crystalY, crystalZ, false, mutableBlockPos),
                    context.calcDamage(crystalX, crystalY, crystalZ, true, mutableBlockPos)
                )
                val collidingDamage = calcCollidingCrystalDamage(crystals, placeBox)
                val adjustedDamage = max(selfDamage, collidingDamage - collidingCrystalExtraSelfDamageThreshold)

                if (player.scaledHealth - adjustedDamage <= noSuicide) continue
                if (player.scaledHealth - collidingDamage <= noSuicide) continue
                if (!lethalOverride && adjustedDamage > placeMaxSelfDamage) continue

                for ((entity, entityPos, entityBox, currentPos) in targets) {
                    if (entityBox.intersects(placeBox)) continue
                    if (placeBox.intersects(entityPos, currentPos)) continue

                    val targetDamage =
                        calcDamage(entity, entityPos, entityBox, crystalX, crystalY, crystalZ, mutableBlockPos)
                    if (lethalOverride && targetDamage - entity.totalHealth > lethalThresholdAddition && selfDamage < lethal.selfDamage && selfDamage <= lethalMaxSelfDamage) {
                        lethal.update(entity, pos, selfDamage, targetDamage)
                    }

                    if (adjustedDamage > placeMaxSelfDamage) continue

                    val minDamage: Float
                    val balance: Float

                    if (shouldForcePlace(entity)) {
                        minDamage = forcePlaceMinDamage
                        balance = forcePlaceBalance
                    } else {
                        minDamage = placeMinDamage
                        balance = placeBalance
                    }

                    if (targetDamage < minDamage || targetDamage - adjustedDamage < balance) continue

                    if (damagePriority(selfDamage, targetDamage) > damagePriority(max.selfDamage, max.targetDamage)) {
                        max.update(entity, pos, adjustedDamage, targetDamage)
                    } else if (max.targetDamage - targetDamage <= safeMaxTargetDamageReduction
                        && max.selfDamage - adjustedDamage >= safeMinSelfDamageReduction
                    ) {
                        safe.update(entity, pos, adjustedDamage, targetDamage)
                    }
                }
            }

            if (max.targetDamage - safe.targetDamage > safeMaxTargetDamageReduction
                || max.selfDamage - safe.selfDamage <= safeMinSelfDamageReduction
            ) {
                safe.clear(player)
            }

            placeInfo = lethal.takeValid()
                ?: safe.takeValid()
                    ?: max.takeValid()

            placeInfo?.calcPlacement(this)
        }

        synchronized(calculationTimes) {
            calculationTimesPending.add(time.toInt())
        }

        return placeInfo
    }

    private fun calcCollidingCrystalDamage(
        crystals: List<Pair<EntityEnderCrystal, CrystalDamage>>,
        placeBox: AxisAlignedBB
    ): Float {
        var max = 0.0f

        for ((crystal, crystalDamage) in crystals) {
            if (!placeBox.intersects(crystal.entityBoundingBox)) continue
            if (crystalDamage.selfDamage > max) {
                max = crystalDamage.selfDamage
            }
        }

        return max
    }

    private fun SafeClientEvent.getTargets(): Sequence<TargetInfo> {
        val rangeSq = targetRange.sq
        val ticks = if (motionPredict) predictTicks else 0
        val list = ArrayList<TargetInfo>()
        val eyePos = PlayerPacketManager.eyePosition

        if (players) {
            for (target in EntityManager.players) {
                if (target == player) continue
                if (target == mc.renderViewEntity) continue
                if (!target.isEntityAlive) continue
                if (target.posY <= 0.0) continue
                if (target.distanceSqTo(eyePos) > rangeSq) continue
                if (FriendManager.isFriend(target.name)) continue

                list.add(getTargetInfo(target, ticks))
            }
        }

        if (mobs || animals) {
            for (target in EntityManager.entity) {
                if (target == player) continue
                if (!target.isEntityAlive) continue
                if (target.posY <= 0.0) continue
                if (target !is EntityLivingBase) continue
                if (target is EntityPlayer) continue
                if (target.distanceSqTo(eyePos) > rangeSq) continue
                if (!animals && target.isPassive) continue

                val pos = target.positionVector
                list.add(
                    TargetInfo(
                        target,
                        pos,
                        target.entityBoundingBox,
                        pos,
                        Vec3d.ZERO,
                        ExposureSample.getExposureSample(target.width, target.height)
                    )
                )
            }
        }

        list.sortBy { player.distanceSqTo(it.entity) }

        return list.asSequence()
            .filter { it.entity.isEntityAlive }
            .take(maxTargets)
    }

    private fun SafeClientEvent.getTargetInfo(entity: EntityLivingBase, ticks: Int): TargetInfo {
        val motionX = (entity.posX - entity.lastTickPosX).coerceIn(-0.6, 0.6)
        val motionY = (entity.posY - entity.lastTickPosY).coerceIn(-0.5, 0.5)
        val motionZ = (entity.posZ - entity.lastTickPosZ).coerceIn(-0.6, 0.6)

        val entityBox = entity.entityBoundingBox
        var targetBox = entityBox

        for (tick in 0..ticks) {
            targetBox = canMove(targetBox, motionX, motionY, motionZ)
                ?: canMove(targetBox, motionX, 0.0, motionZ)
                    ?: canMove(targetBox, 0.0, motionY, 0.0)
                    ?: break
        }

        val offsetX = targetBox.minX - entityBox.minX
        val offsetY = targetBox.minY - entityBox.minY
        val offsetZ = targetBox.minZ - entityBox.minZ
        val motion = Vec3d(offsetX, offsetY, offsetZ)
        val pos = entity.positionVector

        return TargetInfo(
            entity,
            pos.add(motion),
            targetBox,
            pos,
            motion,
            ExposureSample.getExposureSample(entity.width, entity.height)
        )
    }

    private fun SafeClientEvent.canMove(box: AxisAlignedBB, x: Double, y: Double, z: Double): AxisAlignedBB? {
        return box.offset(x, y, z).takeIf { !world.collidesWithAnyBlock(it) }
    }

    private fun SafeClientEvent.shouldForcePlace(entity: EntityLivingBase): Boolean {
        return (!forcePlaceWhileSwording || player.heldItemMainhand.item !is ItemSword)
            && (entity.totalHealth <= forcePlaceHealth
            || entity.realSpeed >= forcePlaceMotion
            || entity.getMinArmorRate() <= forcePlaceArmorRate)
    }

    private fun EntityLivingBase.getMinArmorRate(): Int {
        var minDura = 100

        armorInventoryList.toList().forEachFast { armor ->
            if (!armor.isItemStackDamageable) return@forEachFast
            val dura = armor.duraPercentage
            if (dura < minDura) {
                minDura = dura
            }
        }

        return minDura
    }

    private fun SafeClientEvent.getRawPosList(): List<BlockPos> {
        val mutableBlockPos = BlockPos.MutableBlockPos()

        val range = placeRange

        val rangeSq = range.sq
        val wallRangeSq = wallRange.sq

        val floor = range.floorToInt()
        val ceil = range.ceilToInt()

        val list = ArrayList<BlockPos>()
        val pos = BlockPos.MutableBlockPos()

        val feetPos = PlayerPacketManager.position

        val feetXInt = feetPos.x.floorToInt()
        val feetYInt = feetPos.y.floorToInt()
        val feetZInt = feetPos.z.floorToInt()

        val eyePos = PlayerPacketManager.eyePosition

        for (x in feetXInt - floor..feetXInt + ceil) {
            for (z in feetZInt - floor..feetZInt + ceil) {
                for (y in feetYInt - floor..feetYInt + ceil) {
                    pos.setPos(x, y, z)
                    if (world.isOutsideBuildHeight(pos) || !world.worldBorder.contains(pos)) continue

                    val crystalX = pos.x + 0.5
                    val crystalY = pos.y + 1.0
                    val crystalZ = pos.z + 0.5

                    if (player.placeDistanceSq(crystalX, crystalY, crystalZ) > rangeSq) continue
                    if (!isPlaceable(pos, mutableBlockPos)) continue
                    if (feetPos.distanceSqTo(crystalX, crystalY, crystalZ) > wallRangeSq
                        && !world.rayTraceVisible(eyePos, crystalX, crystalY + 1.7, crystalZ, 20, mutableBlockPos)
                    ) continue

                    list.add(pos.toImmutable())
                }
            }
        }

        list.sortByDescending { it.distanceSqTo(feetXInt, feetYInt, feetZInt) }

        return list
    }

    private fun SafeClientEvent.getPlaceablePos(
        checkRotation: Boolean,
        mutableBlockPos: BlockPos.MutableBlockPos
    ): List<BlockPos> {
        val rangeSq = placeRange.sq

        val single = placeMode == PlaceMode.SINGLE
        val list = ArrayList<BlockPos>()
        val feetPos = PlayerPacketManager.position

        val feetXInt = feetPos.x.floorToInt()
        val feetYInt = feetPos.y.floorToInt()
        val feetZInt = feetPos.z.floorToInt()

        val eyePos = PlayerPacketManager.eyePosition
        val sight = eyePos.add(PlayerPacketManager.rotation.toViewVec().scale(8.0))

        val collidingEntities = getCollidingEntities(rangeSq, feetXInt, feetYInt, feetZInt, single, mutableBlockPos)

        for (pos in rawPosList.get()) {
            if (checkRotation && !checkPlaceRotation(pos, eyePos, sight)) continue
            if (!checkPlaceCollision(pos, collidingEntities)) continue
            list.add(pos)
        }

        return list
    }

    private fun SafeClientEvent.getCollidingEntities(
        rangeSq: Float,
        feetXInt: Int,
        feetYInt: Int,
        feetZInt: Int,
        single: Boolean,
        mutableBlockPos: BlockPos.MutableBlockPos
    ): List<Entity> {
        val collidingEntities = ArrayList<Entity>()
        val rangeSqCeil = rangeSq.ceilToInt()

        for (entity in EntityManager.entity) {
            if (!entity.isEntityAlive) continue

            val adjustedRange = rangeSqCeil - ((entity.width / 2.0f).sq * 2.0f).ceilToInt()
            val dist = entity.distanceSqToCenter(feetXInt, feetYInt, feetZInt)

            if (dist > adjustedRange) continue

            if (entity !is EntityEnderCrystal) {
                collidingEntities.add(entity)
            } else {
                if (!single) {
                    collidingEntities.add(entity)
                } else if (!checkBreakRange(entity, mutableBlockPos)) {
                    collidingEntities.add(entity)
                }
            }
        }

        return collidingEntities
    }

    private fun checkPlaceCollision(
        pos: BlockPos,
        collidingEntities: List<Entity>
    ): Boolean {
        val minX = pos.x + 0.001
        val minY = pos.y + 1.0
        val minZ = pos.z + 0.001
        val maxX = pos.x + 0.999
        val maxY = pos.y + 3.0
        val maxZ = pos.z + 0.999

        return collidingEntities.none {
            it.entityBoundingBox.intersects(minX, minY, minZ, maxX, maxY, maxZ)
        }
    }

    private fun checkPlaceRotation(pos: BlockPos, eyePos: Vec3d, sight: Vec3d): Boolean {
        val grow = Bypass.placeRotationBoundingBoxGrow
        val growPos = 1.0 + grow
        val bb = AxisAlignedBB(
            pos.x - grow, pos.y - grow, pos.z - grow,
            pos.x + growPos, pos.y + growPos, pos.z + growPos
        )
        if (bb.calculateIntercept(eyePos, sight) != null) return true

        return placeRotationRange != 0.0f
            && checkRotationDiff(getRotationTo(eyePos, pos.toVec3dCenter()), placeRotationRange)
    }

    private fun SafeClientEvent.getHandNullable(): EnumHand? {
        return when (Items.END_CRYSTAL) {
            player.heldItemOffhand.item -> EnumHand.OFF_HAND
            player.heldItemMainhand.item -> EnumHand.MAIN_HAND
            else -> null
        }
    }

    private fun SafeClientEvent.swingHand() {
        val hand = when (swingHand) {
            SwingHand.AUTO -> if (player.heldItemOffhand.item.let { it == Items.END_CRYSTAL || it != Items.GOLDEN_APPLE }) EnumHand.OFF_HAND else EnumHand.MAIN_HAND
            SwingHand.OFF_HAND -> EnumHand.OFF_HAND
            SwingHand.MAIN_HAND -> EnumHand.MAIN_HAND
        }

        swingMode.swingHand(this, hand)
    }

    private fun SafeClientEvent.checkBreakRange(
        entity: EntityEnderCrystal,
        mutableBlockPos: BlockPos.MutableBlockPos
    ): Boolean {
        return checkBreakRange(entity.posX, entity.posY, entity.posZ, mutableBlockPos)
    }

    private fun SafeClientEvent.checkBreakRange(
        x: Double,
        y: Double,
        z: Double,
        mutableBlockPos: BlockPos.MutableBlockPos
    ): Boolean {
        return player.breakDistanceSq(x, y, z) <= breakRange.sq
            && (player.distanceSqTo(x, y, z) <= wallRange.sq
            || world.rayTraceVisible(
            player.posX,
            player.posY + player.eyeHeight,
            player.posZ,
            x,
            y + 1.7,
            z,
            20,
            mutableBlockPos
        ))
    }

    private fun Entity.placeDistanceSq(x: Double, y: Double, z: Double): Double {
        return when (placeRangeMode) {
            RangeMode.FEET -> distanceSqTo(x, y, z)
            RangeMode.EYES -> eyeDistanceSq(x, y, z)
        }
    }

    private fun Entity.breakDistanceSq(x: Double, y: Double, z: Double): Double {
        return when (breakRangeMode) {
            RangeMode.FEET -> distanceSqTo(x, y, z)
            RangeMode.EYES -> eyeDistanceSq(x, y, z)
        }
    }

    private fun Entity.eyeDistanceSq(x: Double, y: Double, z: Double): Double {
        return distanceSq(this.posX, this.posY + this.eyeHeight, this.posZ, x, y, z)
    }

    private fun toLong(x: Double, y: Double, z: Double): Long {
        return toLong(x.floorToInt(), y.floorToInt(), z.floorToInt())
    }

    private fun calcDirection(eyePos: Vec3d, hitVec: Vec3d): EnumFacing {
        val x = eyePos.x - hitVec.x
        val y = eyePos.y - hitVec.y
        val z = eyePos.z - hitVec.z

        return EnumFacing.VALUES.maxByOrNull {
            x * it.directionVec.x + y * it.directionVec.y + z * it.directionVec.z
        } ?: EnumFacing.NORTH
    }

    private fun checkRotationDiff(rotation: Vec2f, range: Float): Boolean {
        val serverSide = PlayerPacketManager.rotation
        return RotationUtils.calcAbsAngleDiff(rotation.x, serverSide.x) <= range
            && RotationUtils.calcAbsAngleDiff(rotation.y, serverSide.y) <= range
    }

    private fun SafeClientEvent.isPlaceable(
        pos: BlockPos,
        mutableBlockPos: BlockPos.MutableBlockPos
    ): Boolean {
        if (!canPlaceCrystalOn(pos)) {
            return false
        }
        return hasValidSpaceForCrystal(pos, mutableBlockPos)
    }


    private val reductionMap = Collections.synchronizedMap(WeakHashMap<EntityLivingBase, DamageReduction>())

    private class DamageReduction(entity: EntityLivingBase) {
        private val armorValue: Float = entity.totalArmorValue.toFloat()
        private val toughness: Float
        private val resistance: Float
        private val blastReduction: Float

        init {
            toughness = entity.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).attributeValue
                .toFloat()
            val potionEffect = entity.getActivePotionEffect(MobEffects.RESISTANCE)
            resistance = if (potionEffect != null) max(1.0f - (potionEffect.amplifier + 1) * 0.2f, 0.0f) else 1.0f
            blastReduction = 1.0f - min(calcTotalEPF(entity), 20) / 25.0f
        }

        fun calcReductionDamage(damage: Float): Float {
            return CombatRules.getDamageAfterAbsorb(damage, armorValue, toughness) *
                resistance *
                blastReduction
        }

        companion object {
            private fun calcTotalEPF(entity: EntityLivingBase): Int {
                var epf = 0
                for (itemStack in entity.armorInventoryList) {
                    val nbtTagList = itemStack.enchantmentTagList
                    for (i in 0 until nbtTagList.tagCount()) {
                        val nbtTagCompound = nbtTagList.getCompoundTagAt(i)
                        val id = nbtTagCompound.getInteger("id")
                        val level = nbtTagCompound.getShort("lvl").toInt()
                        if (id == 0) {
                            // Protection
                            epf += level
                        } else if (id == 3) {
                            // Blast protection
                            epf += level * 2
                        }
                    }
                }
                return epf
            }
        }
    }

    private const val DOUBLE_SIZE = 12.0f
    private const val DAMAGE_FACTOR = 42.0f

    private fun SafeClientEvent.calcDamage(
        entity: EntityLivingBase,
        entityPos: Vec3d,
        entityBox: AxisAlignedBB,
        crystalX: Double,
        crystalY: Double,
        crystalZ: Double,
        mutableBlockPos: BlockPos.MutableBlockPos
    ): Float {
        val isPlayer = entity is EntityPlayer
        if (isPlayer && world.difficulty == EnumDifficulty.PEACEFUL) return 0.0f
        var damage: Float

        damage = if (isPlayer
            && crystalY - entityPos.y > 1.5652173822904127
            && checkResistant(
                mutableBlockPos.setPos(
                    crystalX.floorToInt(),
                    crystalY.floorToInt() - 1,
                    crystalZ.floorToInt()
                ),
                world.getBlockState(mutableBlockPos)
            )
        ) {
            1.0f
        } else {
            calcRawDamage(entityPos, entityBox, crystalX, crystalY, crystalZ, mutableBlockPos)
        }

        if (isPlayer) damage = calcDifficultyDamage(world, damage)
        return calcReductionDamage(entity, damage)
    }

    private fun SafeClientEvent.calcRawDamage(
        entityPos: Vec3d,
        entityBox: AxisAlignedBB,
        posX: Double,
        posY: Double,
        posZ: Double,
        mutableBlockPos: BlockPos.MutableBlockPos
    ): Float {
        val scaledDist = entityPos.distanceTo(posX, posY, posZ).toFloat() / DOUBLE_SIZE
        if (scaledDist > 1.0f) return 0.0f

        val factor = (1.0f - scaledDist) * getExposureAmount(entityBox, posX, posY, posZ, mutableBlockPos)
        return ((factor * factor + factor) * DAMAGE_FACTOR + 1.0f)
    }

    private val function = FastRayTraceFunction { pos, blockState ->
        if (checkResistant(pos, blockState)) {
            FastRayTraceAction.CALC
        } else {
            FastRayTraceAction.SKIP
        }
    }

    private fun checkResistant(pos: BlockPos, state: IBlockState): Boolean {
        return CrystalUtils.isResistant(state)
            && (!assumeInstantMine
            || !PacketMine.isInstantMining(pos))
    }

    private fun SafeClientEvent.getExposureAmount(
        entityBox: AxisAlignedBB,
        posX: Double,
        posY: Double,
        posZ: Double,
        mutableBlockPos: BlockPos.MutableBlockPos
    ): Float {
        val width = entityBox.maxX - entityBox.minX
        val height = entityBox.maxY - entityBox.minY

        val gridMultiplierXZ = 1.0 / (width * 2.0 + 1.0)
        val gridMultiplierY = 1.0 / (height * 2.0 + 1.0)

        val gridXZ = width * gridMultiplierXZ
        val gridY = height * gridMultiplierY

        val sizeXZ = (1.0 / gridMultiplierXZ).floorToInt()
        val sizeY = (1.0 / gridMultiplierY).floorToInt()
        val xzOffset = (1.0 - gridMultiplierXZ * (sizeXZ)) / 2.0

        var total = 0
        var count = 0

        for (yIndex in 0..sizeY) {
            for (xIndex in 0..sizeXZ) {
                for (zIndex in 0..sizeXZ) {
                    val x = gridXZ * xIndex + xzOffset + entityBox.minX
                    val y = gridY * yIndex + entityBox.minY
                    val z = gridXZ * zIndex + xzOffset + entityBox.minZ

                    total++
                    if (!world.fastRayTrace(x, y, z, posX, posY, posZ, 20, mutableBlockPos, function)) {
                        count++
                    }
                }
            }
        }

        return count.toFloat() / total.toFloat()
    }

    private fun calcReductionDamage(entity: EntityLivingBase, damage: Float): Float {
        val reduction = reductionMap[entity]
        return reduction?.calcReductionDamage(damage) ?: damage
    }

    private fun calcDifficultyDamage(world: WorldClient, damage: Float): Float {
        return when (world.difficulty) {
            EnumDifficulty.PEACEFUL -> 0.0f
            EnumDifficulty.EASY -> min(damage * 0.5f + 1.0f, damage)
            EnumDifficulty.HARD -> damage * 1.5f
            else -> damage
        }
    }


    private open class PlaceInfo(
        open val target: EntityLivingBase,
        open val blockPos: BlockPos,
        open val selfDamage: Float,
        open val targetDamage: Float,
        open val side: EnumFacing,
        open val hitVecOffset: Vec3f,
        open val hitVec: Vec3d
    ) {
        class Mutable(
            target: EntityLivingBase
        ) : PlaceInfo(
            target,
            BlockPos.ORIGIN,
            Float.MAX_VALUE,
            forcePlaceMinDamage,
            EnumFacing.UP,
            Vec3f.ZERO,
            Vec3d.ZERO
        ) {
            override var target = target; private set
            override var blockPos = super.blockPos; private set
            override var selfDamage = super.selfDamage; private set
            override var targetDamage = super.targetDamage; private set
            override var side = super.side; private set
            override var hitVecOffset = super.hitVecOffset; private set
            override var hitVec = super.hitVec; private set

            fun update(
                target: EntityLivingBase,
                blockPos: BlockPos,
                selfDamage: Float,
                targetDamage: Float
            ) {
                this.target = target
                this.blockPos = blockPos
                this.selfDamage = selfDamage
                this.targetDamage = targetDamage
            }

            fun clear(player: EntityPlayerSP) {
                update(player, BlockPos.ORIGIN, Float.MAX_VALUE, forcePlaceMinDamage)
            }

            fun calcPlacement(event: SafeClientEvent) {
                event {
                    when (placeSideBypass) {
                        PlaceBypass.UP -> {
                            side = EnumFacing.UP
                            hitVecOffset = Vec3f(0.5f, 1.0f, 0.5f)
                            hitVec = Vec3d(blockPos.x + 0.5, blockPos.y + 1.0, blockPos.z + 0.5)
                        }
                        PlaceBypass.DOWN -> {
                            side = EnumFacing.DOWN
                            hitVecOffset = Vec3f(0.5f, 0.0f, 0.5f)
                            hitVec = Vec3d(blockPos.x + 0.5, blockPos.y.toDouble(), blockPos.z + 0.5)
                        }
                        PlaceBypass.CLOSEST -> {
                            side = getMiningSide(blockPos) ?: calcDirection(player.eyePosition, blockPos.toVec3dCenter())
                            val directionVec = side.directionVec
                            val x = directionVec.x * 0.5f + 0.5f
                            val y = directionVec.y * 0.5f + 0.5f
                            val z = directionVec.z * 0.5f + 0.5f
                            hitVecOffset = Vec3f(x, y, z)
                            hitVec = blockPos.toVec3d(x.toDouble(), y.toDouble(), z.toDouble())
                        }
                    }
                }
            }

            fun takeValid(): Mutable? {
                return this.takeIf {
                    target != mc.player
                        && selfDamage != Float.MAX_VALUE
                        && targetDamage != forcePlaceMinDamage
                }
            }
        }

        companion object {
            @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
            @JvmField
            val INVALID = PlaceInfo(object : EntityLivingBase(null) {
                override fun getArmorInventoryList(): MutableIterable<ItemStack> {
                    return ArrayList()
                }

                override fun setItemStackToSlot(slotIn: EntityEquipmentSlot, stack: ItemStack) {

                }

                override fun getItemStackFromSlot(slotIn: EntityEquipmentSlot): ItemStack {
                    return ItemStack.EMPTY
                }

                override fun getPrimaryHand(): EnumHandSide {
                    return EnumHandSide.RIGHT
                }
            }, BlockPos.ORIGIN, Float.NaN, Float.NaN, EnumFacing.UP, Vec3f.ZERO, Vec3d.ZERO)
        }
    }

    private open class BreakInfo(
        open val crystal: EntityEnderCrystal,
        open val selfDamage: Float,
        open val targetDamage: Float
    ) {
        class Mutable : BreakInfo(DUMMY_CRYSTAL, Float.MAX_VALUE, forcePlaceMinDamage) {
            override var crystal = super.crystal; private set
            override var selfDamage = super.selfDamage; private set
            override var targetDamage = super.targetDamage; private set

            fun update(
                target: EntityEnderCrystal,
                selfDamage: Float,
                targetDamage: Float
            ) {
                this.crystal = target
                this.selfDamage = selfDamage
                this.targetDamage = targetDamage
            }

            fun clear() {
                update(DUMMY_CRYSTAL, Float.MAX_VALUE, forcePlaceMinDamage)
            }
        }

        fun takeValid(): BreakInfo? {
            return this.takeIf {
                crystal !== DUMMY_CRYSTAL
                    && selfDamage != Float.MAX_VALUE
                    && targetDamage != forcePlaceMinDamage
            }
        }

        companion object {
            @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
            private val DUMMY_CRYSTAL = EntityEnderCrystal(null, 0.0, 0.0, 0.0)
        }
    }

    private data class TargetInfo(
        val entity: EntityLivingBase,
        val pos: Vec3d,
        val box: AxisAlignedBB,
        val currentPos: Vec3d,
        val predictMotion: Vec3d,
        val exposureSample: ExposureSample
    )

    private object Renderer {
        @JvmField
        var lastBlockPos: BlockPos? = null

        @JvmField
        var prevPos: Vec3d? = null

        @JvmField
        var currentPos: Vec3d? = null

        @JvmField
        var lastRenderPos: Vec3d? = null

        @JvmField
        var lastUpdateTime = 0L

        @JvmField
        var startTime = 0L

        @JvmField
        var scale = 0.0f

        @JvmField
        var lastSelfDamage = 0.0f

        @JvmField
        var lastTargetDamage = 0.0f

        fun reset() {
            lastBlockPos = null
            prevPos = null
            currentPos = null
            lastRenderPos = null
            lastUpdateTime = 0L
            startTime = 0L
            scale = 0.0f
            lastSelfDamage = 0.0f
            lastTargetDamage = 0.0f
        }

        fun onRender3D() {
            val filled = filledAlpha > 0
            val outline = outlineAlpha > 0
            val flag = filled || outline

            if (flag || targetDamage || selfDamage) {
                val placeInfo = renderPlaceInfo
                update(placeInfo)

                prevPos?.let { prevPos ->
                    currentPos?.let { currentPos ->
                        val multiplier = Easing.OUT_QUART.inc(Easing.toDelta(lastUpdateTime, movingLength))
                        val renderPos = prevPos.add(currentPos.subtract(prevPos).scale(multiplier.toDouble()))
                        scale = if (placeInfo != null) {
                            Easing.OUT_CUBIC.inc(Easing.toDelta(startTime, fadeLength))
                        } else {
                            Easing.IN_CUBIC.dec(Easing.toDelta(startTime, fadeLength))
                        }

                        val box = toRenderBox(renderPos, scale)
                        val renderer = ESPRenderer()

                        renderer.aFilled = (filledAlpha * scale).toInt()
                        renderer.aOutline = (outlineAlpha * scale).toInt()
                        renderer.add(box, GuiSetting.primary)
                        renderer.render(false)

                        lastRenderPos = renderPos
                    }
                }
            }
        }

        fun onRender2D() {
            if (scale != 0.0f && (targetDamage || selfDamage)) {
                lastRenderPos?.let {
                    val text = buildString {
                        if (targetDamage) append("%.1f".format(lastTargetDamage))
                        if (selfDamage) {
                            if (this.isNotEmpty()) append('/')
                            append("%.1f".format(lastSelfDamage))
                        }
                    }

                    val screenPos = ProjectionUtils.toAbsoluteScreenPos(it)
                    val alpha = (255.0f * scale).toInt()
                    val color = if (scale == 1.0f) ColorRGB(255, 255, 255) else ColorRGB(255, 255, 255, alpha)

                    MainFontRenderer.drawString(
                        text,
                        screenPos.x.toFloat() - MainFontRenderer.getWidth(text, 2.0f) * 0.5f,
                        screenPos.y.toFloat() - MainFontRenderer.getHeight(2.0f) * 0.5f,
                        color,
                        2.0f
                    )
                }
            }
        }

        private fun toRenderBox(vec3d: Vec3d, scale: Float): AxisAlignedBB {
            val halfSize = 0.5 * scale
            return AxisAlignedBB(
                vec3d.x - halfSize, vec3d.y - halfSize, vec3d.z - halfSize,
                vec3d.x + halfSize, vec3d.y + halfSize, vec3d.z + halfSize
            )
        }

        private fun update(placeInfo: PlaceInfo?) {
            val newBlockPos = placeInfo?.blockPos
            if (newBlockPos != lastBlockPos) {
                if (placeInfo != null) {
                    currentPos = placeInfo.blockPos.toVec3dCenter()
                    prevPos = lastRenderPos ?: currentPos
                    lastUpdateTime = System.currentTimeMillis()
                    if (lastBlockPos == null) startTime = System.currentTimeMillis()
                } else {
                    lastUpdateTime = System.currentTimeMillis()
                    if (lastBlockPos != null) startTime = System.currentTimeMillis()
                }

                lastBlockPos = newBlockPos
            }

            if (placeInfo != null) {
                lastSelfDamage = placeInfo.selfDamage
                lastTargetDamage = placeInfo.targetDamage
            }
        }
    }
}