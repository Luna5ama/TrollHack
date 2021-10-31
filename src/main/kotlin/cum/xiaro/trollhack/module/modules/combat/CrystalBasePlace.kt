package cum.xiaro.trollhack.module.modules.combat

import cum.xiaro.trollhack.util.graphics.ColorRGB
import cum.xiaro.trollhack.event.SafeClientEvent
import cum.xiaro.trollhack.event.events.TickEvent
import cum.xiaro.trollhack.event.events.player.OnUpdateWalkingPlayerEvent
import cum.xiaro.trollhack.event.events.render.Render3DEvent
import cum.xiaro.trollhack.event.listener
import cum.xiaro.trollhack.event.safeListener
import cum.xiaro.trollhack.event.safeParallelListener
import cum.xiaro.trollhack.gui.hudgui.elements.client.Notification
import cum.xiaro.trollhack.manager.managers.CombatManager
import cum.xiaro.trollhack.manager.managers.PlayerPacketManager.sendPlayerPacket
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.util.Bind
import cum.xiaro.trollhack.util.EntityUtils.eyePosition
import cum.xiaro.trollhack.util.TickTimer
import cum.xiaro.trollhack.util.TimeUnit
import cum.xiaro.trollhack.util.combat.CalcContext
import cum.xiaro.trollhack.util.combat.CrystalDamage
import cum.xiaro.trollhack.util.combat.CrystalUtils
import cum.xiaro.trollhack.util.combat.CrystalUtils.hasValidSpaceForCrystal
import cum.xiaro.trollhack.util.graphics.ESPRenderer
import cum.xiaro.trollhack.util.inventory.slot.allSlots
import cum.xiaro.trollhack.util.inventory.slot.firstBlock
import cum.xiaro.trollhack.util.inventory.slot.hasItem
import cum.xiaro.trollhack.util.inventory.slot.hotbarSlots
import cum.xiaro.trollhack.util.math.RotationUtils.getRotationTo
import cum.xiaro.trollhack.util.math.VectorUtils
import cum.xiaro.trollhack.util.math.vector.distanceTo
import cum.xiaro.trollhack.util.math.vector.toVec3d
import cum.xiaro.trollhack.util.threads.defaultScope
import cum.xiaro.trollhack.util.threads.onMainThreadSafeSuspend
import cum.xiaro.trollhack.util.threads.runSafe
import cum.xiaro.trollhack.util.world.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.block.state.IBlockState
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import kotlin.math.max

@CombatManager.CombatModule
internal object CrystalBasePlace : Module(
    name = "CrystalBasePlace",
    description = "Places obby for placing crystal on",
    category = Category.COMBAT,
    modulePriority = 90
) {
    private val manualPlaceBind by setting("Bind Manual Place", Bind(), {
        runSafe {
            if (isEnabled && CombatManager.isOnTopPriority(CrystalBasePlace) && !CombatSetting.pause) {
                prePlace(minDamageIncManual)
            }
        }
    })
    private val minDamageIncManual by setting("Min Damage Inc Inactive", 2.0f, 0.0f..20.0f, 0.25f)
    private val minDamageIncInactive by setting("Min Damage Inc Inactive", 4.0f, 0.0f..20.0f, 0.25f)
    private val minDamageIncActive by setting("Min Damage Inc Active", 8.0f, 0.0f..20.0f, 0.25f)
    private val range by setting("Range", 4.0f, 0.0f..8.0f, 0.5f)
    private val delay by setting("Delay", 20, 0..50, 5)

    private val timer = TickTimer(TimeUnit.TICKS)
    private val renderer = ESPRenderer().apply { aFilled = 33; aOutline = 233 }
    private var inactiveTicks = 0
    private var rotationTo: Vec3d? = null

    override fun isActive(): Boolean {
        return isEnabled && inactiveTicks < 4
    }

    override fun getHudInfo(): String {
        return if (inactiveTicks <= 10) {
            "Active"
        } else {
            ""
        }
    }

    init {
        onDisable {
            inactiveTicks = 0
        }

        listener<Render3DEvent> {
            val clear = inactiveTicks >= 30
            renderer.render(clear)
        }

        safeListener<OnUpdateWalkingPlayerEvent.Pre> {
            if (isActive()) {
                rotationTo?.let { hitVec ->
                    sendPlayerPacket {
                        rotate(getRotationTo(hitVec))
                    }
                }
            } else {
                rotationTo = null
            }
        }

        safeParallelListener<TickEvent.Post> {
            inactiveTicks++

            if (CombatManager.isOnTopPriority(CrystalBasePlace)
                && !CombatSetting.pause
                && TrollAura.isEnabled
                && player.allSlots.hasItem(Items.END_CRYSTAL)) {
                prePlace(if (TrollAura.inactiveTicks > 10) minDamageIncInactive else minDamageIncActive)
            }
        }
    }

    private fun SafeClientEvent.prePlace(minDamageInc: Float) {
        if (rotationTo != null || !timer.tick(delay)) return

        val slot = player.hotbarSlots.firstBlock(Blocks.OBSIDIAN) ?: return
        val eyePos = player.eyePosition
        val posList = VectorUtils.getBlockPosInSphere(eyePos, range)
            .filter { hasValidSpaceForCrystal(it) }
            .filter { world.isPlaceable(it) }
            .toList()

        defaultScope.launch {
            val placeInfo = getPlaceInfo(eyePos, posList, minDamageInc)

            if (placeInfo != null) {
                rotationTo = placeInfo.hitVec
                renderer.replaceAll(mutableListOf(ESPRenderer.Info(AxisAlignedBB(placeInfo.placedPos), ColorRGB(255, 255, 255))))
                inactiveTicks = 0
                timer.reset()

                delay(50)
                onMainThreadSafeSuspend {
                    placeBlock(placeInfo, slot)
                    Notification.send(CrystalBasePlace, "$chatName Obsidian placed")
                }
            } else {
                timer.reset(-max(delay - 1, 0) * 50L)
            }
        }
    }

    private fun SafeClientEvent.getPlaceInfo(eyePos: Vec3d, posList: List<BlockPos>, minDamageInc: Float): PlaceInfo? {
        val contextSelf = CombatManager.contextSelf ?: return null
        val contextTarget = CombatManager.contextTarget ?: return null

        val mutableBlockPos = BlockPos.MutableBlockPos()
        val cacheList = ArrayList<CrystalDamage>()
        val maxCurrentDamage = CombatManager.placeMap.entries
            .filter { eyePos.distanceTo(it.key) < range }
            .maxOfOrNull { it.value.targetDamage } ?: 0.0f

        for (pos in posList) {
            // Neighbor blocks check
            if (!hasNeighbor(pos)) continue

            // Collide check
            val crystalPos = pos.toVec3d(0.5, 1.0, 0.5)
            if (!contextSelf.checkColliding(crystalPos)) continue
            if (!contextTarget.checkColliding(crystalPos)) continue

            // Damage check
            val crystalDamage = calculateDamage(contextSelf, contextTarget, eyePos, crystalPos, pos, mutableBlockPos)
            if (!checkDamage(crystalDamage, maxCurrentDamage, minDamageInc)) continue

            cacheList.add(crystalDamage)
        }

        cacheList.sortByDescending { it.targetDamage }

        for (crystalDamage in cacheList) {
            return getNeighbor(crystalDamage.blockPos, 1) ?: continue
        }

        return null
    }

    private fun calculateDamage(
        contextSelf: CalcContext,
        contextTarget: CalcContext?,
        eyePos: Vec3d,
        crystalPos: Vec3d,
        pos: BlockPos,
        mutableBlockPos: BlockPos.MutableBlockPos
    ): CrystalDamage {
        val function: (BlockPos, IBlockState) -> FastRayTraceAction = { rayTracePos, blockState ->
            when {
                rayTracePos == pos -> {
                    FastRayTraceAction.HIT
                }
                blockState.block != Blocks.AIR && CrystalUtils.isResistant(blockState) -> {
                    FastRayTraceAction.CALC
                }
                else -> {
                    FastRayTraceAction.SKIP
                }
            }
        }

        val selfDamage = max(contextSelf.calcDamage(crystalPos, true, mutableBlockPos, function), contextSelf.calcDamage(crystalPos, false, mutableBlockPos, function))
        val targetDamage = contextTarget?.calcDamage(crystalPos, true, mutableBlockPos, function) ?: 0.0f

        return CrystalDamage(crystalPos, pos, selfDamage, targetDamage, eyePos.distanceTo(crystalPos), contextSelf.currentPos.distanceTo(crystalPos))
    }

    private fun checkDamage(crystalDamage: CrystalDamage, maxCurrentDamage: Float, minDamageInc: Float) =
        crystalDamage.selfDamage <= TrollAura.maxSelfDamage
            && (crystalDamage.targetDamage >= TrollAura.minDamage
            && (crystalDamage.targetDamage - maxCurrentDamage >= minDamageInc))
}