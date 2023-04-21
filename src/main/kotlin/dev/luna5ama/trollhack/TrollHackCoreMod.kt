package dev.luna5ama.trollhack

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin.MCVersion
import org.apache.logging.log4j.LogManager
import org.spongepowered.asm.launch.MixinBootstrap
import org.spongepowered.asm.mixin.MixinEnvironment
import org.spongepowered.asm.mixin.Mixins

@IFMLLoadingPlugin.Name("TrollHackCoreMod")
@MCVersion("1.12.2")
class TrollHackCoreMod : IFMLLoadingPlugin {
    init {
        MixinBootstrap.init()
        Mixins.addConfigurations(
            "mixins.troll.core.json",
            "mixins.troll.accessor.json",
            "mixins.troll.patch.json",
            "mixins.baritone.json"
        )
        MixinEnvironment.getDefaultEnvironment().obfuscationContext = "searge"
        LogManager.getLogger("Troll Hack").info("Troll Hack and Baritone mixins initialised.")
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