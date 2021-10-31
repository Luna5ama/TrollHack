package cum.xiaro.trollhack.util.inventory

import cum.xiaro.trollhack.event.SafeClientEvent
import net.minecraft.inventory.ClickType
import net.minecraft.inventory.Slot

object InstantFuture : StepFuture {
    override fun timeout(timeout: Long): Boolean {
        return true
    }

    override fun confirm() {

    }
}

class Click(
    private val windowID: Int,
    private val slot: Slot,
    private val mouseButton: Int,
    private val type: ClickType
) : Step {
    override fun run(event: SafeClientEvent): ClickFuture {
        val id = event.clickSlot(windowID, slot, mouseButton, type)
        return ClickFuture(id)
    }
}

class ClickFuture(
    val id: Short,
) : StepFuture {
    private val time = System.currentTimeMillis()
    private var confirmed = false

    override fun timeout(timeout: Long): Boolean {
        return confirmed || System.currentTimeMillis() - time > timeout
    }

    override fun confirm() {
        confirmed = true
    }
}

interface Step {
    fun run(event: SafeClientEvent): StepFuture
}

interface StepFuture {
    fun timeout(timeout: Long): Boolean
    fun confirm()
}