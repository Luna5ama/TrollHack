package cum.xiaro.trollhack.util.graphics

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

interface GLObject {
    val id: Int

    fun bind()

    fun unbind()

    fun destroy()
}

@OptIn(ExperimentalContracts::class)
fun <T : GLObject> T.use(block: T.() -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    bind()
    block.invoke(this)
    unbind()
}