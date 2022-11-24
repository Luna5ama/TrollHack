package me.luna.trollhack

import me.luna.trollhack.event.ForgeEventProcessor
import me.luna.trollhack.event.events.ShutdownEvent
import me.luna.trollhack.translation.TranslationManager
import me.luna.trollhack.util.ConfigUtils
import me.luna.trollhack.util.graphics.font.renderer.MainFontRenderer
import me.luna.trollhack.util.threads.BackgroundScope
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.lwjgl.opengl.Display
import java.io.File

@Mod(
    modid = TrollHackMod.ID,
    name = TrollHackMod.NAME,
    version = TrollHackMod.VERSION
)
class TrollHackMod {
    @Suppress("UNUSED_PARAMETER")
    @Mod.EventHandler
    fun preInit(event: FMLPreInitializationEvent) {
        val directory = File("${DIRECTORY}/")
        if (!directory.exists()) directory.mkdir()

        LoaderWrapper.preLoadAll()
        TranslationManager.checkUpdate()

        Thread.currentThread().priority = Thread.MAX_PRIORITY
    }

    @Suppress("UNUSED_PARAMETER")
    @Mod.EventHandler
    fun init(event: FMLInitializationEvent) {
        logger.info("Initializing $NAME $VERSION")

        LoaderWrapper.loadAll()
        MinecraftForge.EVENT_BUS.register(ForgeEventProcessor)
        ConfigUtils.loadAll()
        BackgroundScope.start()
        MainFontRenderer.reloadFonts()

        logger.info("$NAME initialized!")
    }

    @Suppress("UNUSED_PARAMETER")
    @Mod.EventHandler
    fun postInit(event: FMLPostInitializationEvent) {
        ready = true
        Runtime.getRuntime().addShutdownHook(Thread {
            ShutdownEvent.post()
            ConfigUtils.saveAll()
        })
    }

    companion object {
        const val NAME = "Troll Hack"
        const val ID = "trollhack"
        const val VERSION = "0.0.6"
        const val DIRECTORY = "trollhack"

        @JvmField
        val title: String = Display.getTitle()

        @JvmField
        val logger: Logger = LogManager.getLogger(NAME)

        @JvmStatic
        @get:JvmName("isReady")
        var ready = false; private set
    }
}