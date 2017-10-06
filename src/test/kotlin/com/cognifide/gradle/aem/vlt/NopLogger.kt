package com.cognifide.gradle.aem.vlt

import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import org.slf4j.Marker

class NopLogger : Logger {

    override fun warn(p0: String?) {
    }

    override fun warn(p0: String?, p1: Any?) {
    }

    override fun warn(p0: String?, vararg p1: Any?) {
    }

    override fun warn(p0: String?, p1: Any?, p2: Any?) {
    }

    override fun warn(p0: String?, p1: Throwable?) {
    }

    override fun warn(p0: Marker?, p1: String?) {
    }

    override fun warn(p0: Marker?, p1: String?, p2: Any?) {
    }

    override fun warn(p0: Marker?, p1: String?, p2: Any?, p3: Any?) {
    }

    override fun warn(p0: Marker?, p1: String?, vararg p2: Any?) {
    }

    override fun warn(p0: Marker?, p1: String?, p2: Throwable?) {
    }

    override fun isQuietEnabled(): Boolean {
        return false
    }

    override fun getName(): String {
        return "nop"
    }

    override fun info(message: String?, vararg objects: Any?) {
    }

    override fun info(p0: String?) {
    }

    override fun info(p0: String?, p1: Any?) {
    }

    override fun info(p0: String?, p1: Any?, p2: Any?) {
    }

    override fun info(p0: String?, p1: Throwable?) {
    }

    override fun info(p0: Marker?, p1: String?) {
    }

    override fun info(p0: Marker?, p1: String?, p2: Any?) {
    }

    override fun info(p0: Marker?, p1: String?, p2: Any?, p3: Any?) {
    }

    override fun info(p0: Marker?, p1: String?, vararg p2: Any?) {
    }

    override fun info(p0: Marker?, p1: String?, p2: Throwable?) {
    }

    override fun isErrorEnabled(): Boolean {
        return false
    }

    override fun isErrorEnabled(p0: Marker?): Boolean {
        return false
    }

    override fun error(p0: String?) {
    }

    override fun error(p0: String?, p1: Any?) {
    }

    override fun error(p0: String?, p1: Any?, p2: Any?) {
    }

    override fun error(p0: String?, vararg p1: Any?) {
    }

    override fun error(p0: String?, p1: Throwable?) {
    }

    override fun error(p0: Marker?, p1: String?) {
    }

    override fun error(p0: Marker?, p1: String?, p2: Any?) {
    }

    override fun error(p0: Marker?, p1: String?, p2: Any?, p3: Any?) {
    }

    override fun error(p0: Marker?, p1: String?, vararg p2: Any?) {
    }

    override fun error(p0: Marker?, p1: String?, p2: Throwable?) {
    }

    override fun isDebugEnabled(): Boolean {
        return false
    }

    override fun isDebugEnabled(p0: Marker?): Boolean {
        return false
    }

    override fun log(level: LogLevel?, message: String?) {
    }

    override fun log(level: LogLevel?, message: String?, vararg objects: Any?) {
    }

    override fun log(level: LogLevel?, message: String?, throwable: Throwable?) {
    }

    override fun debug(message: String?, vararg objects: Any?) {
    }

    override fun debug(p0: String?) {
    }

    override fun debug(p0: String?, p1: Any?) {
    }

    override fun debug(p0: String?, p1: Any?, p2: Any?) {
    }

    override fun debug(p0: String?, p1: Throwable?) {
    }

    override fun debug(p0: Marker?, p1: String?) {
    }

    override fun debug(p0: Marker?, p1: String?, p2: Any?) {
    }

    override fun debug(p0: Marker?, p1: String?, p2: Any?, p3: Any?) {
    }

    override fun debug(p0: Marker?, p1: String?, vararg p2: Any?) {
    }

    override fun debug(p0: Marker?, p1: String?, p2: Throwable?) {
    }

    override fun isEnabled(level: LogLevel?): Boolean {
        return false
    }

    override fun lifecycle(message: String?) {
    }

    override fun lifecycle(message: String?, vararg objects: Any?) {
    }

    override fun lifecycle(message: String?, throwable: Throwable?) {
    }

    override fun quiet(message: String?) {
    }

    override fun quiet(message: String?, vararg objects: Any?) {
    }

    override fun quiet(message: String?, throwable: Throwable?) {
    }

    override fun isLifecycleEnabled(): Boolean {
        return false
    }

    override fun isInfoEnabled(): Boolean {
        return false
    }

    override fun isInfoEnabled(p0: Marker?): Boolean {
        return false
    }

    override fun trace(p0: String?) {
    }

    override fun trace(p0: String?, p1: Any?) {
    }

    override fun trace(p0: String?, p1: Any?, p2: Any?) {
    }

    override fun trace(p0: String?, vararg p1: Any?) {
    }

    override fun trace(p0: String?, p1: Throwable?) {
    }

    override fun trace(p0: Marker?, p1: String?) {
    }

    override fun trace(p0: Marker?, p1: String?, p2: Any?) {
    }

    override fun trace(p0: Marker?, p1: String?, p2: Any?, p3: Any?) {
    }

    override fun trace(p0: Marker?, p1: String?, vararg p2: Any?) {
    }

    override fun trace(p0: Marker?, p1: String?, p2: Throwable?) {
    }

    override fun isWarnEnabled(): Boolean {
        return false
    }

    override fun isWarnEnabled(p0: Marker?): Boolean {
        return false
    }

    override fun isTraceEnabled(): Boolean {
        return false
    }

    override fun isTraceEnabled(p0: Marker?): Boolean {
        return false
    }

}