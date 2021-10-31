package cum.xiaro.trollhack.command

import cum.xiaro.trollhack.util.command.args.AbstractArg
import cum.xiaro.trollhack.util.command.utils.BuilderBlock
import cum.xiaro.trollhack.util.command.utils.ExecuteBlock
import cum.xiaro.trollhack.event.AlwaysListening
import cum.xiaro.trollhack.event.ClientExecuteEvent
import cum.xiaro.trollhack.event.SafeExecuteEvent
import cum.xiaro.trollhack.gui.hudgui.AbstractHudElement
import cum.xiaro.trollhack.module.AbstractModule
import cum.xiaro.trollhack.module.modules.client.CommandSetting
import cum.xiaro.trollhack.util.PlayerProfile
import cum.xiaro.trollhack.util.Wrapper
import cum.xiaro.trollhack.util.threads.defaultScope
import cum.xiaro.trollhack.util.threads.toSafe
import kotlinx.coroutines.launch
import net.minecraft.block.Block
import net.minecraft.item.Item
import net.minecraft.util.math.BlockPos
import java.io.File

abstract class ClientCommand(
    name: String,
    alias: Array<out String> = emptyArray(),
    description: String = "No description",
) : CommandBuilder<ClientExecuteEvent>(name, alias, description), AlwaysListening {

    val prefixName get() = "$prefix$name"

    @CommandBuilder
    protected inline fun AbstractArg<*>.module(
        name: String,
        block: BuilderBlock<AbstractModule>
    ) {
        arg(ModuleArg(name), block)
    }

    @CommandBuilder
    protected inline fun AbstractArg<*>.hudElement(
        name: String,
        block: BuilderBlock<AbstractHudElement>
    ) {
        arg(HudElementArg(name), block)
    }

    @CommandBuilder
    protected inline fun AbstractArg<*>.block(
        name: String,
        block: BuilderBlock<Block>
    ) {
        arg(BlockArg(name), block)
    }

    @CommandBuilder
    protected inline fun AbstractArg<*>.item(
        name: String,
        block: BuilderBlock<Item>
    ) {
        arg(ItemArg(name), block)
    }

    @CommandBuilder
    protected inline fun AbstractArg<*>.player(
        name: String,
        block: BuilderBlock<PlayerProfile>
    ) {
        arg(PlayerArg(name), block)
    }

    @CommandBuilder
    protected inline fun AbstractArg<*>.blockPos(
        name: String,
        block: BuilderBlock<BlockPos>
    ) {
        arg(BlockPosArg(name), block)
    }

    @CommandBuilder
    protected inline fun AbstractArg<*>.baritoneBlock(
        name: String,
        block: BuilderBlock<Block>
    ) {
        arg(BaritoneBlockArg(name), block)
    }

    @CommandBuilder
    protected inline fun AbstractArg<*>.schematic(
        name: String,
        file: BuilderBlock<File>
    ) {
        arg(SchematicArg(name), file)
    }

    @CommandBuilder
    protected fun AbstractArg<*>.executeAsync(
        description: String = "No description",
        block: ExecuteBlock<ClientExecuteEvent>
    ) {
        val asyncExecuteBlock: ExecuteBlock<ClientExecuteEvent> = {
            defaultScope.launch { block() }
        }
        this.execute(description, block = asyncExecuteBlock)
    }

    @CommandBuilder
    protected fun AbstractArg<*>.executeSafe(
        description: String = "No description",
        block: ExecuteBlock<SafeExecuteEvent>
    ) {
        val safeExecuteBlock: ExecuteBlock<ClientExecuteEvent> = {
            toSafe()?.block()
        }
        this.execute(description, block = safeExecuteBlock)
    }

    protected companion object {
        val mc = Wrapper.minecraft
        val prefix: String get() = CommandSetting.prefix
    }

}