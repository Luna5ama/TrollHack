package dev.luna5ama.trollhack.utils.rotation

enum class Priority(val priority: Int) {
    Lowest(0),
    Low(10),
    Medium(50),
    High(100),
    Highest(1000)
}
