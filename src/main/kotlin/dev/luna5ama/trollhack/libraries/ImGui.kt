package dev.luna5ama.trollhack.libraries

import dev.luna5ama.trollhack.utils.ResourceHelper
import java.nio.file.Files

object ImGui : AbstractLibrary("imgui") {
    override fun load(architecture: Architecture, os: OS): Boolean {
        if (architecture != Architecture.X86_64) throw IllegalStateException("Unsupported arch")
        if (System.getProperty("imgui.library.path") != null) return true

        val libName = when (os) {
            OS.WINDOWS -> "imgui-java64.dll"
            OS.MAC_OS -> "libimgui-java64.dylib"
            OS.LINUX -> "libimgui-java64.so"
            else -> throw IllegalStateException("Unsupported operating system")
        }
        val bundledNative = "io/imgui/java/native-bin/$libName"
        if (ImGui::class.java.classLoader.getResource(bundledNative) != null) return true

        val tempDir = Files.createTempDirectory("trollhack-imgui-natives").toFile()
        val tempFile = tempDir.resolve(libName)
        ResourceHelper.getResourceStream("/assets/trollhack/libraries/$libName").use { input ->
            checkNotNull(input) { "Missing bundled ImGui native library: $libName" }
            tempFile.outputStream().use { output -> input.copyTo(output) }
        }

        tempDir.deleteOnExit()
        tempFile.deleteOnExit()
        System.setProperty("imgui.library.path", tempDir.absolutePath)
        return true
    }
}
