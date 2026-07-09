package dev.luna5ama.trollhack.libraries

import dev.luna5ama.trollhack.utils.Displayable
import oshi.software.os.OperatingSystem.OSVersionInfo
import java.util.*

enum class Environment(val os: OSVersionInfo)

enum class OS(override val displayName: CharSequence) : Displayable {
    ANY("any"),
    LINUX("Linux"),
    MAC_OS("Mac OS"),
    MAC_OS_X("Mac OS X"),
    WINDOWS("Windows"),
    OS2("OS/2"),
    SOLARIS("Solaris"),
    SUN_OS("SunOS"),
    MPE_IX("MPE/iX"),
    HP_UX("HP-UX"),
    AIX("AIX"),
    OS_390("OS/390"),
    FREE_BSD("FreeBSD"),
    IRIX("Irix"),
    DIGITAL_LINUX("Digital Unix"),
    NETWARE("NetWare"),
    OSF1("OSF1"),
    OPEN_VMS("OpenVMS"),
    OTHERS("Others");

    companion object {
        private val _os = System.getProperty("os.name").lowercase(Locale.getDefault())

        fun isLinux() = _os.indexOf("linux") >= 0

        fun isMacOS() = _os.indexOf("mac") >= 0 && _os.indexOf("os") > 0 && _os.indexOf("x") < 0

        fun isMacOSX() = _os.indexOf("mac") >= 0 && _os.indexOf("os") > 0 && _os.indexOf("x") > 0

        fun isWindows() = _os.indexOf("windows") >= 0

        fun isOS2() = _os.indexOf("os/2") >= 0

        fun isSolaris() = _os.indexOf("solaris") >= 0

        fun isSunOS() = _os.indexOf("sunos") >= 0

        fun isMPEiX() = _os.indexOf("mpe/ix") >= 0

        fun isHPUX() = _os.indexOf("hp-ux") >= 0

        fun isAix() = _os.indexOf("aix") >= 0

        fun isOS390() = _os.indexOf("os/390") >= 0

        fun isFreeBSD() = _os.indexOf("freebsd") >= 0

        fun isIrix() = _os.indexOf("irix") >= 0

        fun isDigitalUnix() = _os.indexOf("digital") >= 0 && _os.indexOf("unix") > 0

        fun isNetWare() = _os.indexOf("netware") >= 0

        fun isOSF1() = _os.indexOf("osf1") >= 0

        fun isOpenVMS() = _os.indexOf("openvms") >= 0

        fun detectOs(): OS {
            val platform: OS
            when {
                isAix() -> platform = AIX
                isDigitalUnix() -> platform = DIGITAL_LINUX
                isFreeBSD() -> platform = FREE_BSD
                isHPUX() -> platform = HP_UX
                isIrix() -> platform = IRIX
                isLinux() -> platform = LINUX
                isMacOS() -> platform = MAC_OS
                isMacOSX() -> platform = MAC_OS_X
                isMPEiX() -> platform = MPE_IX
                isNetWare() -> platform = NETWARE
                isOpenVMS() -> platform = OPEN_VMS
                isOS2() -> platform = OS2
                isOS390() -> platform = OS_390
                isOSF1() -> platform = OSF1
                isSolaris() -> platform = SOLARIS
                isSunOS() -> platform = SUN_OS
                isWindows() -> platform = WINDOWS
                else -> platform = OTHERS
            }
            return platform
        }
    }
}

enum class Architecture(override val displayName: CharSequence, val dirName: String) : Displayable {
    X86_64("amd64", "x64"),
    X86("x86", "x86"),
    ARM("ARM", "arm"),
    UNIVERSAL("Universal", "universal");

    companion object {
        private val _arch = System.getProperty("os.arch")

        fun detectArchitecture(): Architecture {
            return when (_arch) {
                "amd64", "x86_64" -> X86_64
                "x86" -> X86
                else -> UNIVERSAL
            }
        }
    }
}