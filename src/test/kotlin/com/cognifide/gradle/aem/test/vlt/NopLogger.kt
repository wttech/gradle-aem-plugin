package com.cognifide.gradle.aem.test.vlt

import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import org.slf4j.helpers.NOPLogger

class NopLogger : NOPLogger(), Logger {

    override fun isQuietEnabled(): Boolean {
        return false
    }

    override fun log(level: LogLevel?, message: String?) {
    }

    override fun log(level: LogLevel?, message: String?, vararg objects: Any?) {
    }

    override fun log(level: LogLevel?, message: String?, throwable: Throwable?) {
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

}