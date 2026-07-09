package dev.luna5ama.trollhack.utils

import dev.luna5ama.trollhack.Metadata
import org.slf4j.Logger
import org.slf4j.Marker
import org.slf4j.event.Level

class SafeLogger(val delegate: Logger) : Logger by delegate {
    override fun isTraceEnabled(marker: Marker?): Boolean = delegate.isTraceEnabled(marker) && !Metadata.GHOST
    override fun isTraceEnabled(): Boolean = delegate.isTraceEnabled && !Metadata.GHOST

    override fun isDebugEnabled(marker: Marker?): Boolean = delegate.isDebugEnabled(marker) && !Metadata.GHOST
    override fun isDebugEnabled(): Boolean = delegate.isDebugEnabled && !Metadata.GHOST

    override fun isInfoEnabled(marker: Marker?): Boolean = delegate.isInfoEnabled(marker) && !Metadata.GHOST
    override fun isInfoEnabled(): Boolean = (delegate.isInfoEnabled && !Metadata.GHOST)

    override fun isWarnEnabled(marker: Marker?): Boolean = delegate.isWarnEnabled(marker) && !Metadata.GHOST
    override fun isWarnEnabled(): Boolean = delegate.isWarnEnabled && !Metadata.GHOST

    override fun isErrorEnabled(marker: Marker?): Boolean = delegate.isErrorEnabled(marker) && !Metadata.GHOST
    override fun isErrorEnabled(): Boolean = delegate.isErrorEnabled && !Metadata.GHOST

    override fun isEnabledForLevel(level: Level?): Boolean = delegate.isEnabledForLevel(level) && !Metadata.GHOST

    override fun getName(): String = delegate.name

    override fun trace(msg: String?) {
        if (!Metadata.GHOST) delegate.trace(msg)
    }

    override fun trace(format: String?, arg: Any?) {
        if (!Metadata.GHOST) delegate.trace(format, arg)
    }

    override fun trace(format: String?, arg1: Any?, arg2: Any?) {
        if (!Metadata.GHOST) delegate.trace(format, arg1, arg2)
    }

    override fun trace(format: String?, vararg arguments: Any?) {
        if (!Metadata.GHOST) delegate.trace(format, arguments)
    }

    override fun trace(msg: String?, t: Throwable?) {
        if (!Metadata.GHOST) delegate.trace(msg, t)
    }

    override fun trace(marker: Marker?, msg: String?) {
        if (!Metadata.GHOST) delegate.trace(marker, msg)
    }

    override fun trace(marker: Marker?, format: String?, arg: Any?) {
        if (!Metadata.GHOST) delegate.trace(marker, format, arg)
    }

    override fun trace(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        if (!Metadata.GHOST) delegate.trace(marker, format, arg1, arg2)
    }

    override fun trace(marker: Marker?, format: String?, vararg argArray: Any?) {
        if (!Metadata.GHOST) delegate.trace(marker, format, argArray)
    }

    override fun trace(marker: Marker?, msg: String?, t: Throwable?) {
        if (!Metadata.GHOST) delegate.trace(marker, msg, t)
    }

    override fun debug(msg: String?) {
        if (!Metadata.GHOST) delegate.debug(msg)
    }

    override fun debug(format: String?, arg: Any?) {
        if (!Metadata.GHOST) delegate.debug(format, arg)
    }

    override fun debug(format: String?, arg1: Any?, arg2: Any?) {
        if (!Metadata.GHOST) delegate.debug(format, arg1, arg2)
    }

    override fun debug(format: String?, vararg arguments: Any?) {
        if (!Metadata.GHOST) delegate.debug(format, arguments)
    }

    override fun debug(msg: String?, t: Throwable?) {
        if (!Metadata.GHOST) delegate.debug(msg, t)
    }

    override fun debug(marker: Marker?, msg: String?) {
        if (!Metadata.GHOST) delegate.debug(marker, msg)
    }

    override fun debug(marker: Marker?, format: String?, arg: Any?) {
        if (!Metadata.GHOST) delegate.debug(marker, format, arg)
    }

    override fun debug(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        if (!Metadata.GHOST) delegate.debug(marker, format, arg1, arg2)
    }

    override fun debug(marker: Marker?, format: String?, vararg argArray: Any?) {
        if (!Metadata.GHOST) delegate.debug(marker, format, argArray)
    }

    override fun debug(marker: Marker?, msg: String?, t: Throwable?) {
        if (!Metadata.GHOST) delegate.debug(marker, msg, t)
    }

    override fun info(msg: String?) {
        if (!Metadata.GHOST) delegate.info(msg)
    }

    override fun info(format: String?, arg: Any?) {
        if (!Metadata.GHOST) delegate.info(format, arg)
    }

    override fun info(format: String?, arg1: Any?, arg2: Any?) {
        if (!Metadata.GHOST) delegate.info(format, arg1, arg2)
    }

    override fun info(format: String?, vararg arguments: Any?) {
        if (!Metadata.GHOST) delegate.info(format, arguments)
    }

    override fun info(msg: String?, t: Throwable?) {
        if (!Metadata.GHOST) delegate.info(msg, t)
    }

    override fun info(marker: Marker?, msg: String?) {
        if (!Metadata.GHOST) delegate.info(marker, msg)
    }

    override fun info(marker: Marker?, format: String?, arg: Any?) {
        if (!Metadata.GHOST) delegate.info(marker, format, arg)
    }

    override fun info(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        if (!Metadata.GHOST) delegate.info(marker, format, arg1, arg2)
    }

    override fun info(marker: Marker?, format: String?, vararg argArray: Any?) {
        if (!Metadata.GHOST) delegate.info(marker, format, argArray)
    }

    override fun info(marker: Marker?, msg: String?, t: Throwable?) {
        if (!Metadata.GHOST) delegate.info(marker, msg, t)
    }

    override fun warn(msg: String?) {
        if (!Metadata.GHOST) delegate.warn(msg)
    }

    override fun warn(format: String?, arg: Any?) {
        if (!Metadata.GHOST) delegate.warn(format, arg)
    }

    override fun warn(format: String?, arg1: Any?, arg2: Any?) {
        if (!Metadata.GHOST) delegate.warn(format, arg1, arg2)
    }

    override fun warn(format: String?, vararg arguments: Any?) {
        if (!Metadata.GHOST) delegate.warn(format, arguments)
    }

    override fun warn(msg: String?, t: Throwable?) {
        if (!Metadata.GHOST) delegate.warn(msg, t)
    }

    override fun warn(marker: Marker?, msg: String?) {
        if (!Metadata.GHOST) delegate.warn(marker, msg)
    }

    override fun warn(marker: Marker?, format: String?, arg: Any?) {
        if (!Metadata.GHOST) delegate.warn(marker, format, arg)
    }

    override fun warn(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        if (!Metadata.GHOST) delegate.warn(marker, format, arg1, arg2)
    }

    override fun warn(marker: Marker?, format: String?, vararg argArray: Any?) {
        if (!Metadata.GHOST) delegate.warn(marker, format, argArray)
    }

    override fun warn(marker: Marker?, msg: String?, t: Throwable?) {
        if (!Metadata.GHOST) delegate.warn(marker, msg, t)
    }

    override fun error(msg: String?) {
        if (!Metadata.GHOST) delegate.error(msg)
    }

    override fun error(format: String?, arg: Any?) {
        if (!Metadata.GHOST) delegate.error(format, arg)
    }

    override fun error(format: String?, arg1: Any?, arg2: Any?) {
        if (!Metadata.GHOST) delegate.error(format, arg1, arg2)
    }

    override fun error(format: String?, vararg arguments: Any?) {
        if (!Metadata.GHOST) delegate.error(format, arguments)
    }

    override fun error(msg: String?, t: Throwable?) {
        if (!Metadata.GHOST) delegate.error(msg, t)
    }

    override fun error(marker: Marker?, msg: String?) {
        if (!Metadata.GHOST) delegate.error(marker, msg)
    }

    override fun error(marker: Marker?, format: String?, arg: Any?) {
        if (!Metadata.GHOST) delegate.error(marker, format, arg)
    }

    override fun error(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
        if (!Metadata.GHOST) delegate.error(marker, format, arg1, arg2)
    }

    override fun error(marker: Marker?, format: String?, vararg argArray: Any?) {
        if (!Metadata.GHOST) delegate.error(marker, format, argArray)
    }

    override fun error(marker: Marker?, msg: String?, t: Throwable?) {
        if (!Metadata.GHOST) delegate.error(marker, msg, t)
    }
}