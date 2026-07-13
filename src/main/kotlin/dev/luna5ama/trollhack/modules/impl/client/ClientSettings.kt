package dev.luna5ama.trollhack.modules.impl.client

import dev.luna5ama.trollhack.gui.TrollClickGui
import dev.luna5ama.trollhack.gui.TrollHudEditor
import dev.luna5ama.trollhack.i18n.Lang
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager
import dev.luna5ama.trollhack.modules.Category
import dev.luna5ama.trollhack.modules.Module
import dev.luna5ama.trollhack.utils.Displayable
import dev.luna5ama.trollhack.utils.EnumGameMode
import java.util.*

object ClientSettings : Module("Client Settings", category = Category.CLIENT) {
    private val page by setting("Page", Page.FAKE_PLAYER)
    val guiScale by setting("GUI Scale", 2.0f, 0.5f..8.0f, 0.25f, { page == Page.RENDER })
    val uid by setting("Origin鈩?ID", true, { page == Page.RENDER })

    val customUUID by setting("Custom UUID", false, { page == Page.FAKE_PLAYER })
    val uuid by setting("UUID", UUID.randomUUID().toString(), { page == Page.FAKE_PLAYER && customUUID })
    val gameMode by setting("Game Mode", EnumGameMode.CREATIVE, { page == Page.FAKE_PLAYER })
    val noResistant by setting("No Resistant", false, { page == Page.FAKE_PLAYER })
    val totem by setting("Totem Supplement", true, { page == Page.FAKE_PLAYER })
    val assumeResistance by setting("Assume Resistance", true, { page == Page.DMG_CALC })
    val horizontalCenterSampling by setting("Horizontal Center Sampling", false, { page == Page.DMG_CALC })
    val verticalCenterSampling by setting("Vertical Center Sampling", true, { page == Page.DMG_CALC })
    val backSideSampling by setting("Back Side Sampling", true, { page == Page.DMG_CALC })

    val ghostHandBypass by setting("Ghost Switch Bypass", HotbarSwitchManager.BypassMode.NONE, { page == Page.BYPASS })
    val antiWeaknessBypass by setting("AntiWeakness Bypass", HotbarSwitchManager.Override.DEFAULT, { page == Page.BYPASS })
    val placeRotationBoundingBoxGrow by setting("Place Rotation Bounding Box Grow", 0.1, 0.0..1.0, 0.01, { page == Page.BYPASS })
    val placeStrictRotation by setting("Place Strict Rotation", true, { page == Page.BYPASS })
    val placeMaxAngle by setting("Place Max Angle", 87, { page == Page.BYPASS })
    val inventorySwapBypass by setting("Inventory Swap Bypass",false, { page == Page.BYPASS })
    val placeMode by setting("Place Mode", PlaceMode.NORMAL, { page == Page.BYPASS })
    val packetPlace by setting("Packet Place",false, { page == Page.BYPASS })
    val randomPitch by setting("Random Pitch", false, { page == Page.BYPASS })
    val boxRevise by setting("Box Revise",0.6, 0.0..1.0, 0.01, { page == Page.BYPASS })
    val yawSpeedLimit by setting("Yaw Speed Limit", 100f, 0f..180f, 5f, { page == Page.BYPASS })

    val predictionUpdateDelay by setting("Motion Predict Update Delay", 100, 0..2000, 50, { page == Page.BACKGROUND_TASKS })
    val normalHolePriority by setting("1x1 Hole Priority", 10, 0..10, 1, { page == Page.BACKGROUND_TASKS })
    val doubleHolePriority by setting("1x2 Hole Priority", 5, 0..10, 1, { page == Page.BACKGROUND_TASKS })
    val doubleDoubleHolePriority by setting("2x2 Hole Priority", 0, 0..10, 1, { page == Page.BACKGROUND_TASKS })
    val holeCalculateDelay by setting("Hole Calc Delay", 10, 0..100, 5, { page == Page.BACKGROUND_TASKS })
    val holeCalculateRange by setting("Hole Calc Range", 20, 0..100, 5, { page == Page.BACKGROUND_TASKS })
    val profilingMaxSamples by setting("Profiling Max Samples", 4, 1..100, 1, { page == Page.BACKGROUND_TASKS })
    val profilingUpdateDelay by setting("Profiling Update Delay", 100, 0..2000, 50, { page == Page.BACKGROUND_TASKS })
    val crystalSetDead by setting("Crystal Set Dead", false, { page == Page.BACKGROUND_TASKS })
    val crystalUpdateDelay by setting("Crystal Update Delay", 25, 5..500, 1, { page == Page.BACKGROUND_TASKS })
    val selfPredictTicks by setting("Self Predict Ticks", 5, 0..20, 1, { page == Page.BACKGROUND_TASKS })
    val targetPredictTicks by setting("Target Predict Ticks", 5, 0..20, 1, { page == Page.BACKGROUND_TASKS })
    val renderThreadTimeThreshold by setting("RT Time Threshold MS", 100, 50..1000, 50, { page == Page.BACKGROUND_TASKS })

    val toggleSound by setting("Toggle Sound", true, { page == Page.MISC })
    val modLanguage by setting("Mod Language", Lang.ENGLISH, { page == Page.MISC }).apply {
        register { _, _ ->
            TrollClickGui.reloadPanel()
            TrollHudEditor.reloadPanel()
            true
        }
    }
    val clientLanguage by setting("Client Language", "en_us", { page == Page.MISC })

    val lowVersion by setting("1.12",false)
    val attackDelay by setting("AttackDelay", 0.2, 0.0..1.0, 0.01, { page == Page.COMBAT })
    val attackRotate by setting("AttackRotate", true, { page == Page.COMBAT })

    private enum class Page(override val displayName: CharSequence) : Displayable {
        RENDER("Render"),
        FAKE_PLAYER("Fake Player"),
        DMG_CALC("Damage Calculation"),
        BYPASS("Bypass"),
        BACKGROUND_TASKS("Background Tasks"),
        MISC("Misc"),
        COMBAT("Attack Delay")
    }

    enum class PlaceMode(override val displayName: CharSequence) : Displayable {
        NORMAL("Normal"),
        STRICT("Strict"),
        LEGIT("Legit"),
        AIR_PLACE("Air Place")
    }
}
