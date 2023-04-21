package dev.luna5ama.trollhack.util.graphics

import dev.fastmc.common.DIRECT_BYTE_BUFFER_CLASS
import dev.fastmc.common.allocateInt
import dev.fastmc.memutil.MemoryArray
import dev.fastmc.memutil.MemoryPointer
import org.lwjgl.opengl.*
import sun.misc.Unsafe
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.nio.ByteBuffer

private val unsafe = run {
    val theUnsafe = Unsafe::class.java.getDeclaredField("theUnsafe")
    theUnsafe.isAccessible = true
    theUnsafe[null] as Unsafe
}

private val trustedLookUp = run {
    val trustedLookupField = MethodHandles.Lookup::class.java.getDeclaredField("IMPL_LOOKUP")
    unsafe.getObject(
        unsafe.staticFieldBase(trustedLookupField),
        unsafe.staticFieldOffset(trustedLookupField)
    ) as MethodHandles.Lookup
}

private val getFunctionAddress = trustedLookUp.findStatic(
    GLContext::class.java,
    "getFunctionAddress",
    MethodType.methodType(
        Long::class.java,
        String::class.java
    )
).run {
    { it: String ->
        this.invokeExact(it) as Long
    }
}

private val nglNamedBufferSubData = trustedLookUp.findStatic(
    GL45::class.java,
    "nglNamedBufferSubData",
    MethodType.methodType(
        Void.TYPE,
        Int::class.java,
        Long::class.java,
        Long::class.java,
        Long::class.java,
        Long::class.java
    )
)

private val glNamedBufferSubDataFunctionPointer = getFunctionAddress("glNamedBufferSubData")

fun glNamedBufferSubData(buffer: Int, offset: Long, dataSize: Long, pointer: MemoryPointer) {
    nglNamedBufferSubData.invokeExact(
        buffer,
        offset,
        dataSize,
        pointer.address,
        glNamedBufferSubDataFunctionPointer
    )
}

fun glNamedBufferSubData(buffer: Int, offset: Long, dataSize: Long, data: Long) {
    nglNamedBufferSubData.invokeExact(buffer, offset, dataSize, data, glNamedBufferSubDataFunctionPointer)
}

private val nglNamedBufferData = trustedLookUp.findStatic(
    GL45::class.java,
    "nglNamedBufferData",
    MethodType.methodType(
        Void.TYPE,
        Int::class.java,
        Long::class.java,
        Long::class.java,
        Int::class.java,
        Long::class.java
    )
)

private val glNamedBufferDataFunctionPointer = getFunctionAddress("glNamedBufferData")

fun glNamedBufferData(buffer: Int, dataSize: Long, pointer: MemoryPointer, usage: Int) {
    nglNamedBufferData.invokeExact(buffer, dataSize, pointer.address, usage, glNamedBufferDataFunctionPointer)
}

fun glNamedBufferData(buffer: Int, dataSize: Long, data: Long, usage: Int) {
    nglNamedBufferData.invokeExact(buffer, dataSize, data, usage, glNamedBufferDataFunctionPointer)
}

private val nglNamedBufferStorage = trustedLookUp.findStatic(
    GL45::class.java,
    "nglNamedBufferStorage",
    MethodType.methodType(
        Void.TYPE,
        Int::class.java,
        Long::class.java,
        Long::class.java,
        Int::class.java,
        Long::class.java
    )
)

private val glNamedBufferStorageFunctionPointer = getFunctionAddress("glNamedBufferStorage")

fun glNamedBufferStorage(buffer: Int, dataSize: Long, pointer: MemoryPointer, flags: Int) {
    nglNamedBufferStorage.invokeExact(
        buffer,
        dataSize,
        pointer.address,
        flags,
        glNamedBufferStorageFunctionPointer
    )
}

fun glNamedBufferStorage(buffer: Int, dataSize: Long, data: Long, flags: Int) {
    nglNamedBufferStorage.invokeExact(buffer, dataSize, data, flags, glNamedBufferStorageFunctionPointer)
}

private val nglDrawElementsBOMethod = trustedLookUp.findStatic(
    GL11::class.java,
    "nglDrawElementsBO",
    MethodType.methodType(
        Void.TYPE,
        Int::class.java,
        Int::class.java,
        Int::class.java,
        Long::class.java,
        Long::class.java
    )
)

private val glDrawElementsFunctionPointer = getFunctionAddress("glDrawElements")

fun glDrawElements(mode: Int, count: Int, type: Int, indices: Long) {
    nglDrawElementsBOMethod.invokeExact(mode, count, type, indices, glDrawElementsFunctionPointer)
}

private val nglMapNamedBufferRange = trustedLookUp.findStatic(
    GL45::class.java,
    "nglMapNamedBufferRange",
    MethodType.methodType(
        ByteBuffer::class.java,
        Int::class.java,
        Long::class.java,
        Long::class.java,
        Int::class.java,
        ByteBuffer::class.java,
        Long::class.java
    )
)

private val glMapNamedBufferRangeFunctionPointer = getFunctionAddress("glMapNamedBufferRange")
private val dummyBuffer = unsafe.allocateInstance(DIRECT_BYTE_BUFFER_CLASS) as ByteBuffer

fun glMapNamedBufferRange(buffer: Int, offset: Long, length: Long, access: Int): MemoryArray {
    val byteBuffer = nglMapNamedBufferRange.invokeExact(
        buffer,
        offset,
        length,
        access,
        dummyBuffer,
        glMapNamedBufferRangeFunctionPointer
    ) as ByteBuffer

    return MemoryArray.wrap(byteBuffer)
}

private val glSyncInstance = unsafe.allocateInstance(GLSync::class.java) as GLSync
private val pointerSetter = trustedLookUp.findSetter(
    GLSync::class.java,
    "pointer",
    Long::class.java
)

private val lengthBuffer = allocateInt(1).apply {
    put(1)
    flip()
}
private val valueBuffer = allocateInt(1)

fun glFenceSync(condition: Int, flags: Int): Long {
    return GL32.glFenceSync(condition, flags).pointer
}

fun glDeleteSync(sync: Long) {
    pointerSetter.invokeExact(glSyncInstance, sync)
    GL32.glDeleteSync(glSyncInstance)
}

fun glGetSynciv(sync: Long, pname: Int): Int {
    pointerSetter.invokeExact(glSyncInstance, sync)
    GL32.glGetSync(glSyncInstance, pname, lengthBuffer, valueBuffer)
    return valueBuffer.get(0)
}