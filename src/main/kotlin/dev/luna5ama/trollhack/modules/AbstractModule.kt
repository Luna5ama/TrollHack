package dev.luna5ama.trollhack.modules

import dev.luna5ama.trollhack.config.Configurable
import dev.luna5ama.trollhack.config.settings.AbstractSetting
import dev.luna5ama.trollhack.event.api.ListenerOwner
import dev.luna5ama.trollhack.i18n.ILocalizedNameable
import dev.luna5ama.trollhack.i18n.LocalizedNameable
import dev.luna5ama.trollhack.manager.managers.ModuleManager
import dev.luna5ama.trollhack.manager.managers.NotificationManager
import dev.luna5ama.trollhack.modules.impl.client.ClientSettings
import dev.luna5ama.trollhack.utils.Describable
import dev.luna5ama.trollhack.utils.Displayable
import dev.luna5ama.trollhack.utils.input.KeyBind
import dev.luna5ama.trollhack.utils.runSafe
import dev.luna5ama.trollhack.utils.sound.SoundPack
import dev.luna5ama.trollhack.config.ConfigCategories
import dev.luna5ama.trollhack.event.api.IListening
import dev.luna5ama.trollhack.gui.hud.impl.Notification
import dev.luna5ama.trollhack.i18n.Lang
import net.minecraft.ChatFormatting
import net.minecraft.sounds.SoundSource
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@JvmDefaultWithoutCompatibility
@Suppress("LeakingThis")
abstract class AbstractModule(
    override val name: CharSequence,
    override val description: CharSequence,
    val category: Category,
    val hidden: Boolean,
    val alwaysListening: Boolean,
    val enableByDefault: Boolean,
    val alwaysEnable: Boolean,
    defaultBind: Int,
    val priority: Int,
    override val alias: Set<CharSequence>,
    var internal: Boolean,
    val excludedFromConfig: Boolean = false
) : IListening by ListenerOwner(),
    ILocalizedNameable by LocalizedNameable(category.resolve(name.toString()), ModuleManager.i18N, name.toString()),
    Describable, Displayable, Configurable by Configurable.NamedConfigurable(
        category.displayName.toString().lowercase(), name.toString().replace(" ", ""),
        configCategory = ConfigCategories.MODULES, excluded = excludedFromConfig
    ), Comparable<AbstractModule> {
    protected val enableConsumers: MutableList<() -> Unit> = ArrayList()
    protected val disableConsumers: MutableList<() -> Unit> = ArrayList()

    val moduleId = register()

    val filteredSettings: List<AbstractSetting<*, *>>
        get() = settings.filter { it != enable0 }

    private val enable0 = setting("Enable", false,
        defaultTranslations = mapOf(Lang.ENGLISH to "Enable", Lang.CHINESE_SIMPLIFIED to "启用"))
    val bind0 = setting("Bind", KeyBind(keyCode = defaultBind), alwaysActive = true,
        defaultTranslations = mapOf(Lang.ENGLISH to "Bind", Lang.CHINESE_SIMPLIFIED to "按键"))
    private val isVisible0 = setting("Visible", true,
        defaultTranslations = mapOf(Lang.ENGLISH to "Visible", Lang.CHINESE_SIMPLIFIED to "可见"))

    var bind by bind0
    var enable by enable0
    val isVisible by isVisible0

    val isEnabled get() = enable0.value
    val isDisabled get() = !isEnabled
    val isActive get() = isEnabled || alwaysListening
    val chatName: String get() = "[${name}]"

    init {
        if (alwaysListening) this.subscribe()

        enableConsumers.add {
            this.subscribe()
            NotificationManager.push(nameAsString, "$nameAsString has been ${ChatFormatting.GREEN}enabled.",
                800, Notification.NotificationType.INFO)
            if (ClientSettings.toggleSound) {
                runSafe {
                    world.playSound(player, player.blockPosition(), SoundPack.ENABLE_SOUNDEVENT,
                        SoundSource.BLOCKS, 1f, 1f)
                }
            }
        }

        disableConsumers.add {
            if (!alwaysListening) this.unsubscribe()
            NotificationManager.push(nameAsString, "$nameAsString has been ${ChatFormatting.RED}disabled.",
                800, Notification.NotificationType.INFO)
            if (ClientSettings.toggleSound) {
                runSafe {
                    world.playSound(player, player.blockPosition(), SoundPack.DISABLE_SOUNDEVENT,
                        SoundSource.BLOCKS, 1f, 1f)
                }
            }
        }

        enable0.register { prev, new ->
            if (new) {
                enableConsumers.forEach { it.invoke() }
            } else {
                disableConsumers.forEach { it.invoke() }
            }

            true
        }

        bind0.onPress {
            this@AbstractModule.toggle()
        }
    }

    override fun compareTo(other: AbstractModule): Int {
        return this.moduleId.compareTo(other.moduleId)
    }

    fun onEnabled(function: () -> Unit) {
        this.enableConsumers.add(function)
    }

    fun onDisabled(function: () -> Unit) {
        this.disableConsumers.add(function)
    }

    fun toggle() {
        this.enable = !this.isEnabled
    }

    fun disable() {
        if (this.isEnabled) this.enable = false
    }

    fun enable() {
        if (this.isDisabled) this.enable = true
    }

    open fun getDisplayInfo(): Any? = null

//    override fun <S : AbstractSetting<*, *>> setting(setting: S): S {
//        println("aaa")
//        return setting.apply { fullSettings.add(setting) }
//    }

    companion object {
        private val moduleIdMap = ConcurrentHashMap<AbstractModule, Int>()
        private val currentId = AtomicInteger(-1)

        fun AbstractModule.register(): Int {
            val id = currentId.incrementAndGet()
            moduleIdMap[this] = id
            return id
        }
    }
}