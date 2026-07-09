/*
 * Copyright (c) 2023-2024 TrollHack 保留所有权利�?All Right Reserved.
 */

package dev.luna5ama.trollhack.utils

import dev.luna5ama.trollhack.TrollHackMod as TrollHackMod
import java.text.SimpleDateFormat
import java.util.*

object TimeUtil {

    @JvmStatic
    fun isEvening(): Boolean {
        when (val today = SimpleDateFormat("HH").format(Date()).toInt()) {
            1, 2, 3, 4, 5, 6, 19, 20, 21, 22, 23, 0 -> {
                TrollHackMod.LOGGER.info("It's night, Time is $today")
                return true
            }
            7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18 -> {
                TrollHackMod.LOGGER.info("It's not night, Time is $today")
                return false
            }
        }
        TrollHackMod.LOGGER.info("Unknown time...")
        return false
    }

}