package com.cognifide.gradle.aem.common.instance.action.check

import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.action.CheckAction

class Checks(
    val action: CheckAction,
    val instance: Instance,
    val checks: Set<Check>
) {

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

    val status: String
        get() = checks.firstOrNull { it.failure }?.status ?: "<no status>"
}