package dev.luna5ama.trollhack.utils.timing

enum class NanoTimeUnit(val multiplier: Long) {
    Nanosecond(1),
    Microsecond(1000),
    Millisecond(1000000),
    Second(1000000000),
    Minute(60000000000),
    Hour(3600000000000),
    Day(86400000000000),
    Week(604800000000000)
}