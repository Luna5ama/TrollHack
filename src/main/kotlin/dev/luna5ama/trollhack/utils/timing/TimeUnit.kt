package dev.luna5ama.trollhack.utils.timing

enum class TimeUnit(val multiplier: Long) {
    MILLISECONDS(1L),
    TICKS(50L),
    SECONDS(1000L),
    MINUTES(60000L),
    HOURS(60000L * 60L),
    DAYS(60000L * 60L * 24L);

    companion object {
        fun from(unit: java.util.concurrent.TimeUnit): TimeUnit {
            return when (unit) {
                java.util.concurrent.TimeUnit.NANOSECONDS -> TODO()
                java.util.concurrent.TimeUnit.MICROSECONDS -> TODO()
                java.util.concurrent.TimeUnit.MILLISECONDS -> MILLISECONDS
                java.util.concurrent.TimeUnit.SECONDS -> SECONDS
                java.util.concurrent.TimeUnit.MINUTES -> MINUTES
                java.util.concurrent.TimeUnit.HOURS -> HOURS
                java.util.concurrent.TimeUnit.DAYS -> DAYS
            }
        }
    }
}