package dev.luna5ama.trollhack.manager.managers

import kotlinx.coroutines.*
import dev.luna5ama.trollhack.I18NManager
import dev.luna5ama.trollhack.TrollHackMod as TrollHackMod
import dev.luna5ama.trollhack.TrollHackMod.resolve
import dev.luna5ama.trollhack.config.settings.BindSetting
import dev.luna5ama.trollhack.gui.hud.impl.ActiveModules
import dev.luna5ama.trollhack.gui.hud.impl.HudArrayList
import dev.luna5ama.trollhack.gui.hud.impl.HudEventPosts
import dev.luna5ama.trollhack.gui.hud.impl.HudFPS
import dev.luna5ama.trollhack.gui.hud.impl.Notification
import dev.luna5ama.trollhack.i18n.ILocalizedNameable
import dev.luna5ama.trollhack.i18n.LocalizedNameable
import dev.luna5ama.trollhack.manager.AbstractManager
import dev.luna5ama.trollhack.modules.*
import dev.luna5ama.trollhack.modules.impl.client.*
import dev.luna5ama.trollhack.modules.impl.combat.AutoTotem
import dev.luna5ama.trollhack.modules.impl.combat.Criticals
import dev.luna5ama.trollhack.modules.impl.combat.KillAura
import dev.luna5ama.trollhack.modules.impl.combat.MaceSpoof
import dev.luna5ama.trollhack.modules.impl.combat.zc.ZealotCrystal
import dev.luna5ama.trollhack.modules.impl.misc.AntiItemCrash
import dev.luna5ama.trollhack.modules.impl.misc.FakePlayer
import dev.luna5ama.trollhack.modules.impl.misc.FastPlace
import dev.luna5ama.trollhack.modules.impl.misc.HitSound
import dev.luna5ama.trollhack.modules.impl.misc.HitboxDesync
import dev.luna5ama.trollhack.modules.impl.movement.ElytraFlight
import dev.luna5ama.trollhack.modules.impl.movement.ElytraFlightNew
import dev.luna5ama.trollhack.modules.impl.movement.Flight
import dev.luna5ama.trollhack.modules.impl.movement.GuiMove
import dev.luna5ama.trollhack.modules.impl.movement.NoFall
import dev.luna5ama.trollhack.modules.impl.movement.NoSlowDown
import dev.luna5ama.trollhack.modules.impl.movement.SafeWalk
import dev.luna5ama.trollhack.modules.impl.movement.Sprint
import dev.luna5ama.trollhack.modules.impl.movement.Velocity
import dev.luna5ama.trollhack.modules.impl.player.AntiHunger
import dev.luna5ama.trollhack.modules.impl.player.AutoRespawn
import dev.luna5ama.trollhack.modules.impl.player.MultiTask
import dev.luna5ama.trollhack.modules.impl.player.NoEntityTrace
import dev.luna5ama.trollhack.modules.impl.player.NoRotate
import dev.luna5ama.trollhack.modules.impl.player.PacketMine
import dev.luna5ama.trollhack.modules.impl.player.Scaffold
import dev.luna5ama.trollhack.modules.impl.player.Search
import dev.luna5ama.trollhack.modules.impl.visual.AspectRatio
import dev.luna5ama.trollhack.modules.impl.visual.BlockHighlight
import dev.luna5ama.trollhack.modules.impl.visual.BlueArchiveHalo
import dev.luna5ama.trollhack.modules.impl.visual.CrystalDamage
import dev.luna5ama.trollhack.modules.impl.visual.FullBright
import dev.luna5ama.trollhack.modules.impl.visual.MotionBlur
import dev.luna5ama.trollhack.modules.impl.visual.NameTags
import dev.luna5ama.trollhack.modules.impl.visual.NoCameraClip
import dev.luna5ama.trollhack.modules.impl.visual.NoRender
import dev.luna5ama.trollhack.modules.impl.visual.PlaceRender
import dev.luna5ama.trollhack.modules.impl.visual.RenderTest
import dev.luna5ama.trollhack.modules.impl.visual.Shaders
import dev.luna5ama.trollhack.modules.impl.visual.Tracers
import dev.luna5ama.trollhack.modules.impl.visual.valkyrie.Valkyrie
import dev.luna5ama.trollhack.script.FabricPlatform
import dev.luna5ama.trollhack.utils.Profiler
import dev.luna5ama.trollhack.utils.input.KeyBind
import dev.luna5ama.trollhack.utils.runSafe
import dev.luna5ama.trollhack.utils.threads.Coroutine
import org.luaj.vm2.compiler.LuaC

object ModuleManager : AbstractManager(), ILocalizedNameable by LocalizedNameable(resolve("modules"), I18NManager.i18N) {
    val modules by lazy { mutableListOf(
        ClickGui,
        ClientSettings,
        Colors,
        GraphicsInfo,
        HudEditor,
        HurtTimeDebug,
        PacketDebug,
        Presets,
        RefreshFontCache,
        ReloadScript,
        Watermark,

        ZealotCrystal,
        AutoTotem,
        Criticals,
        KillAura,
        MaceSpoof,

        AntiItemCrash,
        FakePlayer,
        FastPlace,
        HitboxDesync,
        HitSound,

        ElytraFlight,
        ElytraFlightNew,
        Flight,
        GuiMove,
        NoFall,
        NoSlowDown,
        SafeWalk,
        Sprint,
        Velocity,

        AntiHunger,
        AutoRespawn,
        MultiTask,
        NoEntityTrace,
        NoRotate,
        PacketMine,
        Scaffold,
        Search,

        Valkyrie,
        AspectRatio,
        BlockHighlight,
        BlueArchiveHalo,
        CrystalDamage,
        FullBright,
        MotionBlur,
        NameTags,
        NoCameraClip,
        NoRender,
        Notification,
        PlaceRender,
        RenderTest,
        Shaders,
        Tracers,

        ActiveModules,
        HudArrayList,
        HudEventPosts,
        HudFPS,
        Notification
    ) }

    fun getModuleByName(name: CharSequence) = getFilteredModule().find { it.nameAsString.equals(name.toString(), true) }

    fun getModulesByCategory(category: Category) = getFilteredModule().filter { it.category == category }

    fun getEnabledModules() = modules.filter { it.isEnabled }

    private fun getFilteredModule() = modules.filter { !it.internal }

    fun onKeyPressed(keyCode: KeyBind) =
        modules.associateWith { require(it.settings.isNotEmpty())
            it.settings.filterIsInstance<BindSetting>() }
            .forEach { (module, bList) ->
                bList.filter {
                    it.value.keyCode == keyCode.keyCode
                }.forEach { b ->
                    b.isPressed = true
                    if (module.isEnabled || b.alwaysActive) b.onPressConsumers.forEach { func ->
                        runSafe(func)
                    }
                }
            }

    fun onKeyReleased(keyCode: KeyBind) =
        modules.associateWith { it.settings.filterIsInstance<BindSetting>() }
            .forEach { (_, bList) ->
                bList.filter {
                    it.value.keyCode == keyCode.keyCode
                }.forEach { b ->
                    b.isPressed = false
                }
            }

    fun loadScript() {
        val scriptsFolder = TrollHackMod.FOLDER.resolve("scripts")
        if (!scriptsFolder.exists()) {
            scriptsFolder.mkdirs()
            return
        }
        runBlocking {
            scriptsFolder.listFiles()?.filter { it.extension.lowercase() == "lua" }?.map { f ->
                Coroutine.async {
                    try {
                        f.inputStream().use { source ->
                            val code = f.readText()
                            if (code.startsWith("-- trollhack-scripts")) {
                                val prototype = LuaC.instance.compile(source, f.name)
                                val initialEnv = FabricPlatform.emptyGlobals()
                                initialEnv.loader.load(prototype, f.name, initialEnv).call()
                                val module = LuaModule(code, prototype, initialEnv)
                                TrollHackMod.LOGGER.info("Loaded script ${module.name}")
                                modules.add(module)
                            }
                        }
                    } catch (e: Exception) {
                        TrollHackMod.LOGGER.warn("Failed to load script ${f.name}, skipping", e)
                    }
                }
            }?.awaitAll() ?: TrollHackMod.LOGGER.warn("Cannot read script folder")
        }
    }

    override fun load(profilerScope: Profiler.ProfilerScope) {
        modules.forEach {
            it.apply {
                if (it::class.java.isAnnotationPresent(ExperimentalModule::class.java)) onEnabled {
                    NotificationManager.push(
                        nameAsString,
                        "You are using an experimental module!",
                        1000,
                        Notification.NotificationType.WARN
                    )
                }
                if (alwaysListening) subscribe()
                if (enableByDefault) enable = true
            }
        }
        loadScript()
        val m = modules.distinctBy { it.moduleId }
        modules.clear()
        modules.addAll(m)
        TrollHackMod.LOGGER.info("${modules.size} module(s) were loaded")
    }
}