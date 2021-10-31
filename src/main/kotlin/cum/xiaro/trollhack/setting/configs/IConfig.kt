package cum.xiaro.trollhack.setting.configs

import cum.xiaro.trollhack.util.interfaces.Nameable
import java.io.File

/**
 * Setting group that can be saved to a .json file
 */
interface IConfig : Nameable {

    /** Main file of the config */
    val file: File

    /** Backup file of the config */
    val backup: File

    /**
     * Save this group to its .json file
     */
    fun save()

    /**
     * Load all setting values in from its .json file
     */
    fun load()

}