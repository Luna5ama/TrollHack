package cum.xiaro.trollhack.module.modules.render

import cum.xiaro.trollhack.module.Category
import cum.xiaro.trollhack.module.Module
import cum.xiaro.trollhack.util.and
import cum.xiaro.trollhack.util.atTrue
import cum.xiaro.trollhack.util.atValue
import net.minecraft.client.entity.AbstractClientPlayer
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumHand
import net.minecraft.util.EnumHandSide

internal object ViewModel : Module(
    name = "ViewModel",
    alias = arrayOf("ItemModel", "SmallShield", "LowerOffhand"),
    description = "Modify hand rendering in first person",
    category = Category.RENDER
) {
    private val separatedHand0 = setting("Separated Hand", false)
    private val separatedHand by separatedHand0
    private val totem0 = setting("Totem", false)
    private val totem by totem0
    private val page = setting("Page", Page.POSITION)

    private val posX by setting("Pos X", 0.0f, -5.0f..5.0f, 0.025f, page.atValue(Page.POSITION))
    private val posY by setting("Pos Y", 0.0f, -5.0f..5.0f, 0.025f, page.atValue(Page.POSITION))
    private val posZ by setting("Pos Z", 0.0f, -5.0f..5.0f, 0.025f, page.atValue(Page.POSITION))
    private val posXR by setting("Pos X Right", 0.0f, -5.0f..5.0f, 0.025f, page.atValue(Page.POSITION) and separatedHand0.atTrue())
    private val posYR by setting("Pos Y Right", 0.0f, -5.0f..5.0f, 0.025f, page.atValue(Page.POSITION) and separatedHand0.atTrue())
    private val posZR by setting("Pos Z Right", 0.0f, -5.0f..5.0f, 0.025f, page.atValue(Page.POSITION) and separatedHand0.atTrue())
    private val posXT by setting("Pos X Totem", 0.0f, -5.0f..5.0f, 0.025f, page.atValue(Page.POSITION) and totem0.atTrue())
    private val posYT by setting("Pos Y Totem", 0.0f, -5.0f..5.0f, 0.025f, page.atValue(Page.POSITION) and totem0.atTrue())
    private val posZT by setting("Pos Z Totem", 0.0f, -5.0f..5.0f, 0.025f, page.atValue(Page.POSITION) and totem0.atTrue())

    private val rotateX by setting("Rotate X", 0.0f, -180.0f..180.0f, 1.0f, page.atValue(Page.ROTATION))
    private val rotateY by setting("Rotate Y", 0.0f, -180.0f..180.0f, 1.0f, page.atValue(Page.ROTATION))
    private val rotateZ by setting("Rotate Z", 0.0f, -180.0f..180.0f, 1.0f, page.atValue(Page.ROTATION))
    private val rotateXR by setting("Rotate X Right", 0.0f, -180.0f..180.0f, 1.0f, page.atValue(Page.ROTATION) and separatedHand0.atTrue())
    private val rotateYR by setting("Rotate Y Right", 0.0f, -180.0f..180.0f, 1.0f, page.atValue(Page.ROTATION) and separatedHand0.atTrue())
    private val rotateZR by setting("Rotate Z Right", 0.0f, -180.0f..180.0f, 1.0f, page.atValue(Page.ROTATION) and separatedHand0.atTrue())
    private val rotateXT by setting("Rotate X Totem", 0.0f, -180.0f..180.0f, 1.0f, page.atValue(Page.ROTATION) and totem0.atTrue())
    private val rotateYT by setting("Rotate Y Totem", 0.0f, -180.0f..180.0f, 1.0f, page.atValue(Page.ROTATION) and totem0.atTrue())
    private val rotateZT by setting("Rotate Z Totem", 0.0f, -180.0f..180.0f, 1.0f, page.atValue(Page.ROTATION) and totem0.atTrue())

    private val scale by setting("Scale", 1.0f, 0.1f..3.0f, 0.025f, page.atValue(Page.SCALE))
    private val scaleR by setting("Scale Right", 1.0f, 0.1f..3.0f, 0.025f, page.atValue(Page.SCALE) and separatedHand0.atTrue())
    private val scaleT by setting("Scale Totem", 1.0f, 0.1f..3.0f, 0.025f, page.atValue(Page.SCALE) and totem0.atTrue())


    private val modifyHand by setting("Modify Hand", false)

    private enum class Page {
        POSITION, ROTATION, SCALE
    }

    @JvmStatic
    fun translate(stack: ItemStack, hand: EnumHand, player: AbstractClientPlayer) {
        if (isDisabled || !modifyHand && stack.isEmpty) return

        val enumHandSide = getEnumHandSide(player, hand)

        when {
            totem && player.getHeldItem(hand).item == Items.TOTEM_OF_UNDYING -> {
                translate(posXT, posYT, posZT, getSideMultiplier(enumHandSide))
            }
            separatedHand -> {
                if (enumHandSide == EnumHandSide.LEFT) {
                    translate(posX, posY, posZ, -1.0f)
                } else {
                    translate(posXR, posYR, posZR, 1.0f)
                }
            }
            else -> {
                translate(posX, posY, posZ, getSideMultiplier(enumHandSide))
            }
        }
    }

    private fun translate(x: Float, y: Float, z: Float, sideMultiplier: Float) {
        GlStateManager.translate(x * sideMultiplier, y, -z)
    }

    @JvmStatic
    fun rotateAndScale(stack: ItemStack, hand: EnumHand, player: AbstractClientPlayer) {
        if (isDisabled || !modifyHand && stack.isEmpty) return

        val enumHandSide = getEnumHandSide(player, hand)

        when {
            totem && player.getHeldItem(hand).item == Items.TOTEM_OF_UNDYING -> {
                rotate(rotateXT, rotateYT, rotateZT, getSideMultiplier(enumHandSide))
                GlStateManager.scale(scaleT, scaleT, scaleT)
            }
            separatedHand -> {
                if (enumHandSide == EnumHandSide.LEFT) {
                    rotate(rotateX, rotateY, rotateZ, -1.0f)
                    GlStateManager.scale(scale, scale, scale)
                } else {
                    rotate(rotateXR, rotateYR, rotateZR, 1.0f)
                    GlStateManager.scale(scaleR, scaleR, scaleR)
                }
            }
            else -> {
                rotate(rotateX, rotateY, rotateZ, getSideMultiplier(enumHandSide))
                GlStateManager.scale(scale, scale, scale)
            }
        }
    }

    private fun rotate(x: Float, y: Float, z: Float, sideMultiplier: Float) {
        GlStateManager.rotate(x, 1.0f, 0.0f, 0.0f)
        GlStateManager.rotate(y * sideMultiplier, 0.0f, 1.0f, 0.0f)
        GlStateManager.rotate(z * sideMultiplier, 0.0f, 0.0f, 1.0f)
    }

    private fun getEnumHandSide(player: AbstractClientPlayer, hand: EnumHand): EnumHandSide =
        if (hand == EnumHand.MAIN_HAND) player.primaryHand else player.primaryHand.opposite()

    private fun getSideMultiplier(side: EnumHandSide) =
        if (side == EnumHandSide.LEFT) -1.0f else 1.0f
}