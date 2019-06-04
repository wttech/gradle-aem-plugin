package com.cognifide.gradle.aem.common.instance.check

import com.cognifide.gradle.aem.common.build.CollectingLogger
import com.cognifide.gradle.aem.common.instance.Instance

class CheckGroup(
    val runner: CheckRunner,
    val instance: Instance,
    checkFactory: CheckGroup.() -> List<Check>
) {

    val statusLogger = CollectingLogger()

    var checks: List<Check> = checkFactory(this)

    @Suppress("TooGenericExceptionCaught")
    fun check() {
        for (check in checks) {
            try {
                check.check()
            } catch (e: Exception) {
                runner.abortCause = e
                break
            }
        }
    }

    val done: Boolean
        get() = checks.all { it.success }

    val summary: String
        get() = checks.firstOrNull { it.failure }?.status ?: "checks passed"
}