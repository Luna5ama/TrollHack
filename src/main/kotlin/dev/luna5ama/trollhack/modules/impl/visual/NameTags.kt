package dev.luna5ama.trollhack.modules.impl.visual

import dev.luna5ama.trollhack.event.api.nonNullHandler
import dev.luna5ama.trollhack.event.impl.render.Skia2DEvent
import dev.luna5ama.trollhack.graphics.blaze3d.WorldProjection
import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import dev.luna5ama.trollhack.manager.managers.FriendManager
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import net.minecraft.world.phys.Vec3
import java.util.Locale

object NameTags : Module("Name Tags", category = Category.RENDER) {
    private val range by setting("Range", 64.0f, 4.0f..128.0f, 1.0f)
    private val scale by setting("Scale", 0.8f, 0.4f..2.0f, 0.1f)
    private val heightOffset by setting("Height Offset", 0.15f, -0.5f..1.0f, 0.05f)
    private val backgroundColor by setting("Background Color", ColorRGBA(0, 0, 0, 130))
    val vanillaNameTags by setting("Vanilla Name Tags", false)
    private val showSelf by setting("Show Self", true)

    init {
        nonNullHandler<Skia2DEvent> { event ->
            val partial = event.ticksDelta
            val maxDistance = range.toDouble() * range
            for (target in world.players()) {
                if (!target.isAlive || target.isSpectator || target == player && (!showSelf || mc.options.cameraType.isFirstPerson)) continue
                if (player.distanceToSqr(target) > maxDistance) continue
                val x = net.minecraft.util.Mth.lerp(partial.toDouble(), target.xo, target.x)
                val y = net.minecraft.util.Mth.lerp(partial.toDouble(), target.yo, target.y) + target.bbHeight + heightOffset
                val z = net.minecraft.util.Mth.lerp(partial.toDouble(), target.zo, target.z)
                val screen = WorldProjection.worldToScreen(Vec3(x, y, z), event.framebufferWidth, event.framebufferHeight, event.width, event.height) ?: continue
                val name = target.name.string
                val health = String.format(Locale.ROOT, "[%.1f HP]", target.health + target.absorptionAmount)
                val text = "$name $health"
                val textSize = 12f * scale
                val width = event.draw.measureText(text, textSize)
                val height = event.draw.textHeight(textSize)
                event.draw.rect(screen.x - width * 0.5f - 3f, screen.y - height - 3f, screen.x + width * 0.5f + 3f, screen.y + 3f, backgroundColor)
                val color = if (FriendManager.isFriend(name)) ColorRGBA(20, 255, 20, 235) else ColorRGBA(255, 255, 255, 235)
                event.draw.centeredText(text, screen.x, screen.y - height, textSize, color, shadow = true)
            }
        }
    }
}
