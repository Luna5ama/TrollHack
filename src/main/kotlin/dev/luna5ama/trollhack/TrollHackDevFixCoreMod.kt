package dev.luna5ama.trollhack

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin.MCVersion
import org.apache.logging.log4j.LogManager
import org.spongepowered.asm.launch.MixinBootstrap
import org.spongepowered.asm.mixin.MixinEnvironment
import org.spongepowered.asm.mixin.Mixins

@IFMLLoadingPlugin.Name("TrollHackDevFixCoreMod")
@MCVersion("1.12.2")
class TrollHackDevFixCoreMod : IFMLLoadingPlugin {
    private val enableMod = true

    init {
        MixinBootstrap.init()
        MixinEnvironment.getDefaultEnvironment().obfuscationContext = "searge"
        if (enableMod) {
            Mixins.addConfigurations(
                "mixins.troll.core.json",
                "mixins.troll.accessor.json",
                "mixins.troll.patch.json",
                "mixins.troll.devfix.json",
                "mixins.baritone.json"
            )
            LogManager.getLogger("Troll Hack").info("Troll Hack and Baritone mixins initialised.")
        } else {
            Mixins.addConfigurations(
                "mixins.troll.devfix.json"
            )
        }
    }

    override fun injectData(data: Map<String, Any>) {

    }

    override fun getASMTransformerClass(): Array<String> {
        return emptyArray()
    }

    override fun getModContainerClass(): String? {
        return null
    }

    override fun getSetupClass(): String? {
        return null
    }

    override fun getAccessTransformerClass(): String? {
        return null
    }
}
