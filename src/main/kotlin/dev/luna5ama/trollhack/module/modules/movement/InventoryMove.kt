package dev.luna5ama.trollhack.module.modules.movement

import dev.luna5ama.trollhack.TrollHackMod
import dev.luna5ama.trollhack.event.events.player.InputUpdateEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.gui.AbstractTrollGui
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.gui.GuiRepair
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.inventory.GuiEditSign
import net.minecraft.util.MovementInputFromOptions
import org.lwjgl.input.Keyboard

internal object InventoryMove : Module(
    name = "Inventory Move",
    description = "Allows you to walk around with GUIs opened",
    category = Category.MOVEMENT
) {
    private val rotateSpeed by setting("Rotate Speed", 5, 0..20, 1)
    val sneak by setting("Sneak", false)

    private var hasSent = false

    init {
        safeListener<InputUpdateEvent>(9999) {
            if (it.movementInput !is MovementInputFromOptions || isInvalidGui(mc.currentScreen)) return@safeListener

            if (Keyboard.isKeyDown(Keyboard.KEY_LEFT)) {
                player.rotationYaw = player.rotationYaw - rotateSpeed
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_RIGHT)) {
                player.rotationYaw = player.rotationYaw + rotateSpeed
            }

            // pitch can not exceed 90 degrees nor -90 degrees, otherwise AAC servers will flag this and kick you.
            if (Keyboard.isKeyDown(Keyboard.KEY_UP)) {
                player.rotationPitch = (player.rotationPitch - rotateSpeed).coerceAtLeast(-90.0f)
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_DOWN)) {
                player.rotationPitch = (player.rotationPitch + rotateSpeed).coerceAtMost(90.0f)
            }

            it.movementInput.moveStrafe = 0.0f
            it.movementInput.moveForward = 0.0f

            try {
                if (Keyboard.isKeyDown(mc.gameSettings.keyBindForward.keyCode)) {
                    ++it.movementInput.moveForward
                    it.movementInput.forwardKeyDown = true
                } else {
                    it.movementInput.forwardKeyDown = false
                }

                if (Keyboard.isKeyDown(mc.gameSettings.keyBindBack.keyCode)) {
                    --it.movementInput.moveForward
                    it.movementInput.backKeyDown = true
                } else {
                    it.movementInput.backKeyDown = false
                }

                if (Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.keyCode)) {
                    ++it.movementInput.moveStrafe
                    it.movementInput.leftKeyDown = true
                } else {
                    it.movementInput.leftKeyDown = false
                }

                if (Keyboard.isKeyDown(mc.gameSettings.keyBindRight.keyCode)) {
                    --it.movementInput.moveStrafe
                    it.movementInput.rightKeyDown = true
                } else {
                    it.movementInput.rightKeyDown = false
                }

                if (Keyboard.isKeyDown(mc.gameSettings.keyBindJump.keyCode)) {
                    it.movementInput.jump = true
                }

                if (Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.keyCode) && sneak) {
                    it.movementInput.sneak = true
                }
            } catch (e: IndexOutOfBoundsException) {
                if (!hasSent) {
                    TrollHackMod.logger.error("$chatName Error: Key is bound to a mouse button!", e)
                    hasSent = true
                }
            }
        }
    }

    private fun isInvalidGui(guiScreen: GuiScreen?) = guiScreen == null
        || guiScreen is GuiChat
        || guiScreen is GuiEditSign
        || guiScreen is GuiRepair
        || guiScreen.let { it is AbstractTrollGui && it.searching }
}