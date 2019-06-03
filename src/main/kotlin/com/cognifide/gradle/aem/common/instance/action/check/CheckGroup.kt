package com.cognifide.gradle.aem.common.instance.action.check

import com.cognifide.gradle.aem.common.build.CollectingLogger
import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.action.CheckAction

class CheckGroup(
    val action: CheckAction,
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
                action.error = e
                break
            }
        }
    }

    val done: Boolean
        get() = checks.all { it.success }

    val summary: String
        get() = checks.firstOrNull { it.failure }?.status ?: "checks passed"
}