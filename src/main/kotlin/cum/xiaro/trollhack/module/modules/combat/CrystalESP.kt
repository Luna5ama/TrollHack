package cum.xiaro.trollhack.module.modules.combat

import cum.xiaro.trollhack.util.graphics.ColorRGB
import cum.xiaro.trollhack.util.math.MathUtils
import cum.xiaro.trollhack.event.SafeClientEvent
import cum.xiaro.trollhack.event.events.PacketEvent
import cum.xiaro.trollhack.event.events.TickEvent
import cum.xiaro.trollhack.event.events.render.Render2DEvent
import cum.xiaro.trollhack.event.events.render.Render3DEvent
import cum.xiaro.trollhack.event.listener
import cum.xiaro.trollhack.event.safeListener
import cum.xiaro.trollhack.event.safeParallelListener
import cum.xiaro.trollhack.manager.managers.CombatManager
import cum.xiaro.trollhack.manager.managers.HotbarManager.serverSideItem
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.util.Quad
import cum.xiaro.trollhack.util.combat.CrystalUtils.placeCollideCheck
import cum.xiaro.trollhack.util.graphics.ESPRenderer
import cum.xiaro.trollhack.util.graphics.ProjectionUtils
import cum.xiaro.trollhack.util.graphics.RenderUtils3D
import cum.xiaro.trollhack.util.graphics.font.renderer.MainFontRenderer
import cum.xiaro.trollhack.util.math.vector.toVec3dCenter
import cum.xiaro.trollhack.util.threads.runSynchronized
import it.unimi.dsi.fastutil.longs.Long2LongMaps
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap
import net.minecraft.entity.item.EntityEnderCrystal
import net.minecraft.init.Items
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock
import net.minecraft.util.EnumHand
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import org.lwjgl.opengl.GL11.*
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin

internal object CrystalESP : Module(
    name = "CrystalESP",
    description = "Renders ESP for End Crystals",
    category = Category.COMBAT
) {
    private val onlyOwn by setting("Only Own", false)
    private val filled by setting("Filled", true)
    private val outline by setting("Outline", true)
    private val showDamage by setting("Damage", true)
    private val showSelfDamage by setting("Self Damage", true)
    private val animationScale by setting("Animation Scale", 1.0f, 0.0f..2.0f, 0.1f)
    private val crystalRange by setting("Range", 16.0f, 0.0f..16.0f, 0.5f)
    private val r by setting("Red", 133, 0..255, 1)
    private val g by setting("Green", 255, 0..255, 1)
    private val b by setting("Blue", 200, 0..255, 1)
    private val aFilled by setting("Filled Alpha", 47, 0..255, 1, ::filled)
    private val aOutline by setting("Outline Alpha", 127, 0..255, 1, ::outline)
    private val width by setting("Width", 2.0f, 0.25f..4.0f, 0.25f, ::outline)

    private var renderCrystalMap = emptyMap<BlockPos, Quad<Float, Float, Float, Float>>() // <Crystal, <Target Damage, Self Damage, Prev Progress, Progress>>
    private val selfPlaced = Long2LongMaps.synchronize(Long2LongOpenHashMap())

    override fun getHudInfo(): String {
        return renderCrystalMap.size.toString()
    }

    init {
        onDisable {
            renderCrystalMap = emptyMap()
            selfPlaced.clear()
        }

        safeListener<PacketEvent.PostSend>(0) { event ->
            if (!onlyOwn || event.packet !is CPacketPlayerTryUseItemOnBlock) return@safeListener

            if (checkHeldItem(event.packet) && placeCollideCheck(event.packet.pos) { it is EntityEnderCrystal }) {
                selfPlaced[event.packet.pos.toLong()] = System.currentTimeMillis() + 500L
            }
        }

        safeParallelListener<TickEvent.Post> {
            val removeTime = System.currentTimeMillis()

            selfPlaced.runSynchronized {
                values.removeIf {
                    it < removeTime
                }
            }

            val newMap = HashMap<BlockPos, Quad<Float, Float, Float, Float>>()

            for ((_, calculation) in CombatManager.crystalMap) {
                if (calculation.eyeDistance > crystalRange) continue
                if (onlyOwn && !selfPlaced.containsKey(calculation.blockPos.toLong())) continue
                newMap[calculation.blockPos] = Quad(calculation.targetDamage, calculation.selfDamage, 0.0f, 0.0f)
            }

            val scale = 1.0f / animationScale
            for ((pos, quad1) in renderCrystalMap) {
                newMap.computeIfPresent(pos) { _, quad2 ->
                    Quad(quad2.first, quad2.second, quad1.fourth, min(quad1.fourth + 0.4f * scale, 1.0f))
                }
                if (quad1.fourth < 2.0f) {
                    newMap.computeIfAbsent(pos) {
                        Quad(quad1.first, quad1.second, quad1.fourth, min(quad1.fourth + 0.2f * scale, 2.0f))
                    }
                }
            }

            renderCrystalMap = newMap
        }

        listener<Render3DEvent> {
            if (renderCrystalMap.isNotEmpty()) {
                val renderer = ESPRenderer()

                renderer.aFilled = if (filled) aFilled else 0
                renderer.aOutline = if (outline) aOutline else 0
                renderer.thickness = width

                for ((pos, quad) in renderCrystalMap) {
                    val progress = getAnimationProgress(quad.third, quad.fourth)
                    val box = AxisAlignedBB(pos).shrink(0.5 - progress * 0.5)
                    val color = ColorRGB(r, g, b, (progress * 255.0f).toInt())
                    renderer.add(box, color)
                }

                renderer.render(false)
            }
        }

        listener<Render2DEvent.Absolute> {
            if (!showDamage && !showSelfDamage) return@listener

            for ((pos, quad) in renderCrystalMap) {
                glPushMatrix()

                val screenPos = ProjectionUtils.toAbsoluteScreenPos(pos.toVec3dCenter())
                glTranslated(screenPos.x, screenPos.y, 0.0)
                glScalef(2.0f, 2.0f, 1.0f)

                val alpha = (getAnimationProgress(quad.third, quad.fourth) * 255f).toInt()
                val color = ColorRGB(255, 255, 255, alpha)

                val text = buildString {
                    if (showDamage) append(abs(MathUtils.round(quad.first, 1)))
                    if (showSelfDamage) {
                        if (this.isNotEmpty()) append('/')
                        append(abs(MathUtils.round(quad.second, 1)))
                    }
                }

                val x = MainFontRenderer.getWidth(text) * -0.5f
                val y = MainFontRenderer.getHeight() * -0.5f
                MainFontRenderer.drawString(text, x, y, color = color)

                glPopMatrix()
            }
        }
    }

    private fun SafeClientEvent.checkHeldItem(packet: CPacketPlayerTryUseItemOnBlock): Boolean {
        return packet.hand == EnumHand.MAIN_HAND && player.serverSideItem.item == Items.END_CRYSTAL
            || packet.hand == EnumHand.OFF_HAND && player.heldItemOffhand.item == Items.END_CRYSTAL
    }

    private fun getAnimationProgress(prevProgress: Float, progress: Float): Float {
        val interpolated = prevProgress + (progress - prevProgress) * RenderUtils3D.partialTicks
        return sin(interpolated * 0.5 * PI).toFloat()
    }
}