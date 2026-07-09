package dev.luna5ama.trollhack.command

import kotlinx.coroutines.launch
import dev.luna5ama.trollhack.command.args.AbstractArg
import dev.luna5ama.trollhack.event.api.AlwaysListening
import dev.luna5ama.trollhack.event.api.ClientExecuteContext
import dev.luna5ama.trollhack.event.api.NonNullExecuteContext
import dev.luna5ama.trollhack.event.api.toSafe
import dev.luna5ama.trollhack.modules.AbstractModule
import dev.luna5ama.trollhack.utils.MinecraftWrapper
import dev.luna5ama.trollhack.utils.threads.Coroutine
import net.minecraft.core.BlockPos

abstract class ClientCommand(
    name: String,
    alias: Array<out String> = emptyArray(),
    description: String = "No description",
) : CommandBuilder<ClientExecuteContext>(name, alias, description), AlwaysListening {

    val prefixName get() = "$prefix$name"

    @CommandBuilder
    protected inline fun AbstractArg<*>.module(
        name: String,
        block: BuilderBlock<AbstractModule>
    ) {
        arg(ModuleArg(name), block)
    }

//    @CommandBuilder
//    protected inline fun AbstractArg<*>.block(
//        name: String,
//        block: BuilderBlock<Block>
//    ) {
//        arg(BlockArg(name), block)
//    }
//
//    @CommandBuilder
//    protected inline fun AbstractArg<*>.item(
//        name: String,
//        block: BuilderBlock<Item>
//    ) {
//        arg(ItemArg(name), block)
//    }


    @CommandBuilder
    protected inline fun AbstractArg<*>.blockPos(
        name: String,
        block: BuilderBlock<BlockPos>
    ) {
        arg(BlockPosArg(name), block)
    }

    @CommandBuilder
    protected fun AbstractArg<*>.executeAsync(
        description: String = "No description",
        block: ExecuteBlock<ClientExecuteContext>
    ) {
        val asyncExecuteBlock: ExecuteBlock<ClientExecuteContext> = {
            Coroutine.launch { block() }
        }
        this.execute(description, block = asyncExecuteBlock)
    }

    @CommandBuilder
    protected fun AbstractArg<*>.executeSafe(
        description: String = "No description",
        block: ExecuteBlock<NonNullExecuteContext>
    ) {
        val safeExecuteBlock: ExecuteBlock<ClientExecuteContext> = {
            toSafe()?.block()
        }
        this.execute(description, block = safeExecuteBlock)
    }

    protected companion object {
        val mc = MinecraftWrapper.minecraft
        val prefix: String get() = CommandManager.prefix.toString()
    }

}