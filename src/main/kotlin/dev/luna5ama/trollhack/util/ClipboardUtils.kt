package dev.luna5ama.trollhack.util

import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection

object ClipboardUtils {
    fun copyToClipboard(text: String) {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val selection = StringSelection(text)
        clipboard.setContents(selection, selection)
    }

    fun pasteFromClipboard(): String {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val data = clipboard.getData(DataFlavor.stringFlavor)
        return data as String? ?: ""
    }
}