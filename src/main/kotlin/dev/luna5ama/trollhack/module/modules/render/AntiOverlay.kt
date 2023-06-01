package dev.luna5ama.trollhack.module.modules.render

import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.events.render.RenderBlockOverlayEvent
import dev.luna5ama.trollhack.event.events.render.RenderOverlayEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import net.minecraft.client.tutorial.TutorialSteps
import net.minecraft.init.MobEffects
import net.minecraftforge.client.event.RenderGameOverlayEvent

internal object AntiOverlay : Module(
    name = "Anti Overlay",
    description = "Prevents rendering of fire, water and block texture overlays.",
    category = Category.RENDER
) {
    val hurtCamera = setting("Hurt Camera", true)
    private val fire = setting("Fire", true)
    private val water = setting("Water", true)
    private val blocks = setting("Blocks", true)
    private val portals = setting("Portals", true)
    private val blindness = setting("Blindness", true)
    private val nausea = setting("Nausea", true)
    val totems = setting("Totems", true)
    private val vignette = setting("Vignette", false)
    private val helmet = setting("Helmet", true)
    private val tutorial = setting("Tutorial", true)
    private val potionIcons = setting("Potion Icons", false)

    init {
        listener<RenderBlockOverlayEvent> {
            it.cancelled = when (it.type) {
                net.minecraftforge.client.event.RenderBlockOverlayEvent.OverlayType.FIRE -> fire.value
                net.minecraftforge.client.event.RenderBlockOverlayEvent.OverlayType.WATER -> water.value
                net.minecraftforge.client.event.RenderBlockOverlayEvent.OverlayType.BLOCK -> blocks.value
                else -> it.cancelled
            }
        }

        listener<RenderOverlayEvent.Pre> {
            it.cancelled = when (it.type) {
                RenderGameOverlayEvent.ElementType.VIGNETTE -> vignette.value
                RenderGameOverlayEvent.ElementType.PORTAL -> portals.value
                RenderGameOverlayEvent.ElementType.HELMET -> helmet.value
                RenderGameOverlayEvent.ElementType.POTION_ICONS -> potionIcons.value
                else -> it.cancelled
            }
        }

        safeListener<TickEvent.Pre> {
            if (blindness.value) player.removeActivePotionEffect(MobEffects.BLINDNESS)
            if (nausea.value) player.removeActivePotionEffect(MobEffects.NAUSEA)
            if (tutorial.value) mc.gameSettings.tutorialStep = TutorialSteps.NONE
        }
    }
}