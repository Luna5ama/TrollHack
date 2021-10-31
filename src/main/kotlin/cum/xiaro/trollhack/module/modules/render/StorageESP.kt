package cum.xiaro.trollhack.module.modules.render

import cum.xiaro.trollhack.util.extension.sq
import cum.xiaro.trollhack.util.graphics.ColorRGB
import cum.xiaro.trollhack.event.SafeClientEvent
import cum.xiaro.trollhack.event.events.TickEvent
import cum.xiaro.trollhack.event.events.render.Render3DEvent
import cum.xiaro.trollhack.event.listener
import cum.xiaro.trollhack.event.safeParallelListener
import cum.xiaro.trollhack.manager.managers.EntityManager
import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.util.EntityUtils.eyePosition
import cum.xiaro.trollhack.util.and
import cum.xiaro.trollhack.util.atTrue
import cum.xiaro.trollhack.util.atValue
import cum.xiaro.trollhack.util.graphics.GlStateUtils
import cum.xiaro.trollhack.util.graphics.esp.DynamicBoxRenderer
import cum.xiaro.trollhack.util.graphics.esp.DynamicTracerRenderer
import cum.xiaro.trollhack.util.graphics.esp.StaticBoxRenderer
import cum.xiaro.trollhack.util.graphics.esp.StaticTracerRenderer
import cum.xiaro.trollhack.util.graphics.mask.SideMask
import cum.xiaro.trollhack.util.math.vector.distanceSqTo
import cum.xiaro.trollhack.util.or
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import net.minecraft.entity.Entity
import net.minecraft.entity.item.*
import net.minecraft.item.ItemShulkerBox
import net.minecraft.tileentity.*
import org.lwjgl.opengl.GL11.*

internal object StorageESP : Module(
    name = "StorageESP",
    description = "Draws an ESP on top of storage units",
    category = Category.RENDER
) {
    private val page = setting("Page", Page.TYPE)

    /* Type settings */
    private val chest by setting("Chest", true, page.atValue(Page.TYPE))
    private val shulker by setting("Shulker", true, page.atValue(Page.TYPE))
    private val enderChest by setting("Ender Chest", true, page.atValue(Page.TYPE))
    private val frame0 = setting("Item Frame", true, page.atValue(Page.TYPE))
    private val frame by frame0
    private val withShulkerOnly by setting("With Shulker Only", true, page.atValue(Page.TYPE) and frame0.atTrue())
    private val furnace by setting("Furnace", false, page.atValue(Page.TYPE))
    private val dispenser by setting("Dispenser", false, page.atValue(Page.TYPE))
    private val hopper by setting("Hopper", false, page.atValue(Page.TYPE))
    private val cart by setting("Minecart", false, page.atValue(Page.TYPE))
    private val range by setting("Range", 64.0f, 8.0f..128.0f, 4.0f, page.atValue(Page.TYPE))

    /* Color settings */
    private val colorChest by setting("Chest Color", ColorRGB(255, 132, 32), true, page.atValue(Page.COLOR))
    private val colorDispenser by setting("Dispenser Color", ColorRGB(160, 160, 160), true, page.atValue(Page.COLOR))
    private val colorShulker by setting("Shulker Color", ColorRGB(220, 64, 220), true, page.atValue(Page.COLOR))
    private val colorEnderChest by setting("Ender Chest Color", ColorRGB(137, 50, 184), true, page.atValue(Page.COLOR))
    private val colorFurnace by setting("Furnace Color", ColorRGB(160, 160, 160), true, page.atValue(Page.COLOR))
    private val colorHopper by setting("Hopper Color", ColorRGB(80, 80, 80), true, page.atValue(Page.COLOR))
    private val colorCart by setting("Cart Color", ColorRGB(32, 250, 32), true, page.atValue(Page.COLOR))
    private val colorFrame by setting("Frame Color", ColorRGB(255, 132, 32), true, page.atValue(Page.COLOR))

    /* Render settings */
    private val filled0 = setting("Filled", true, page.atValue(Page.RENDER))
    private val filled by filled0
    private val outline0 = setting("Outline", true, page.atValue(Page.RENDER))
    private val outline by outline0
    private val tracer0 = setting("Tracer", true, page.atValue(Page.RENDER))
    private val tracer by tracer0
    private val filledAlpha by setting("Filled Alpha", 63, 0..255, 1, page.atValue(Page.RENDER) and filled0.atTrue())
    private val outlineAlpha by setting("Outline Alpha", 200, 0..255, 1, page.atValue(Page.RENDER) and outline0.atTrue())
    private val tracerAlpha by setting("Tracer Alpha", 200, 0..255, 1, page.atValue(Page.RENDER) and tracer0.atTrue())
    private val lineWidth by setting("Line Width", 2.0f, 0.25f..5.0f, 0.25f, page.atValue(Page.RENDER) and (outline0.atTrue() or tracer0.atTrue()))

    private enum class Page {
        TYPE, COLOR, RENDER
    }

    override fun getHudInfo(): String {
        return (dynamicBoxRenderer.size + staticBoxRenderer.size).toString()
    }

    private val dynamicBoxRenderer = DynamicBoxRenderer()
    private val staticBoxRenderer = StaticBoxRenderer()
    private val dynamicTracerRenderer = DynamicTracerRenderer()
    private val staticTracerRenderer = StaticTracerRenderer()

    init {
        listener<Render3DEvent> {
            glHint(GL_LINE_SMOOTH_HINT, GL_NICEST)
            glLineWidth(lineWidth)
            GlStateUtils.depth(false)

            val filledAlpha = if (filled) filledAlpha else 0
            val outlineAlpha = if (outline) outlineAlpha else 0
            val tracerAlpha = if (tracer) tracerAlpha else 0

            dynamicBoxRenderer.render(filledAlpha, outlineAlpha)
            staticBoxRenderer.render(filledAlpha, outlineAlpha)
            dynamicTracerRenderer.render(tracerAlpha)
            staticTracerRenderer.render(tracerAlpha)

            GlStateUtils.cull(true)
            GlStateUtils.depth(true)
            glLineWidth(1.0f)
        }

        safeParallelListener<TickEvent.Post> {
            coroutineScope {
                launch {
                    updateTileEntities()
                }
                launch {
                    updateEntities()
                }
            }
        }
    }

    private fun SafeClientEvent.updateTileEntities() {
        val eyePos = player.eyePosition
        val rangeSq = range.sq

        staticBoxRenderer.update {
            staticTracerRenderer.update {
                for (tileEntity in world.loadedTileEntityList.toList()) {
                    if (eyePos.distanceSqTo(tileEntity.pos) > rangeSq) continue
                    if (!checkTileEntityType(tileEntity)) continue

                    val color = getTileEntityColor(tileEntity)
                    if (color.rgba == 0) continue

                    val box = world.getBlockState(tileEntity.pos).getSelectedBoundingBox(world, tileEntity.pos)
                        ?: continue
                    var sideMask = SideMask.ALL

                    if (tileEntity is TileEntityChest) {
                        // Leave only the colliding face and then flip the bits (~) to have ALL but that face
                        if (tileEntity.adjacentChestZNeg != null) sideMask -= SideMask.NORTH
                        if (tileEntity.adjacentChestZPos != null) sideMask -= SideMask.SOUTH
                        if (tileEntity.adjacentChestXNeg != null) sideMask -= SideMask.WEST
                        if (tileEntity.adjacentChestXPos != null) sideMask -= SideMask.EAST
                    }

                    putBox(box, color, sideMask, sideMask.toOutlineMaskInv())
                    putTracer(box, color)
                }
            }
        }
    }

    private fun checkTileEntityType(tileEntity: TileEntity): Boolean {
        return chest && tileEntity is TileEntityChest
            || dispenser && tileEntity is TileEntityDispenser
            || shulker && tileEntity is TileEntityShulkerBox
            || enderChest && tileEntity is TileEntityEnderChest
            || furnace && tileEntity is TileEntityFurnace
            || hopper && tileEntity is TileEntityHopper
    }

    private fun getTileEntityColor(tileEntity: TileEntity): ColorRGB {
        return when (tileEntity) {
            is TileEntityChest -> colorChest
            is TileEntityDispenser -> colorDispenser
            is TileEntityShulkerBox -> colorShulker
            is TileEntityEnderChest -> colorEnderChest
            is TileEntityFurnace -> colorFurnace
            is TileEntityHopper -> colorHopper
            else -> ColorRGB(0, 0, 0, 0)
        }
    }

    private fun SafeClientEvent.updateEntities() {
        val eyePos = player.eyePosition
        val rangeSq = range.sq

        dynamicBoxRenderer.update {
            dynamicTracerRenderer.update {
                for (entity in EntityManager.entity) {
                    if (entity.distanceSqTo(eyePos) > rangeSq) continue
                    if (!checkEntityType(entity)) continue

                    val box = entity.entityBoundingBox ?: continue
                    val color = getEntityColor(entity)
                    if (color.rgba == 0) continue

                    val xOffset = entity.posX - entity.lastTickPosX
                    val yOffset = entity.posY - entity.lastTickPosY
                    val zOffset = entity.posZ - entity.lastTickPosZ

                    putBox(box, xOffset, yOffset, zOffset, color)
                    putTracer(entity.posX, entity.posY, entity.posZ, xOffset, yOffset, zOffset, color)
                }
            }
        }
    }

    private fun checkEntityType(entity: Entity): Boolean {
        return frame && entity is EntityItemFrame && (!withShulkerOnly || entity.displayedItem.item is ItemShulkerBox)
            || cart && (entity is EntityMinecartChest || entity is EntityMinecartHopper || entity is EntityMinecartFurnace)
    }

    private fun getEntityColor(entity: Entity): ColorRGB {
        return when (entity) {
            is EntityMinecartContainer -> colorCart
            is EntityItemFrame -> colorFrame
            else -> ColorRGB(0, 0, 0, 0)
        }
    }
}
