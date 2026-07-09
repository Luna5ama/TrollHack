package dev.luna5ama.trollhack.utils

object SafetyWarnings {
    fun build(obj: String, reasons: List<String>): String {
        return buildString {
            append("$obj was detected due to security problem:")
            for (reason in reasons) {
                append("\t $reason;")
            }
            append("Skip this message if you know what you are doing.")
        }
    }
}