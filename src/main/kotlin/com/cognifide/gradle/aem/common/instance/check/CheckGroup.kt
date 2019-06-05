package com.cognifide.gradle.aem.common.instance.check

import com.cognifide.gradle.aem.common.build.CollectingLogger
import com.cognifide.gradle.aem.common.instance.Instance
import org.apache.commons.lang3.builder.HashCodeBuilder

class CheckGroup(
    val runner: CheckRunner,
    val instance: Instance,
    checkFactory: CheckGroup.() -> List<Check>
) {

    val stateBuilder = HashCodeBuilder()

    val statusLogger = CollectingLogger()

    var checks: List<Check> = checkFactory(this)

    @Suppress("TooGenericExceptionCaught")
    fun check() {
        for (check in checks) {
            try {
                check.check()
                if (check.failure) {
                    break
                }
            } catch (e: Exception) {
                runner.abortCause = e
                break
            }
        }
    }

    fun log() {
        statusLogger.entries.forEach {
            runner.aem.logger.log(it.level, it.details)
        }
    }

    fun state(value: Any) {
        stateBuilder.append(value)
    }

    val state: Int
        get() = stateBuilder.toHashCode()

    val done: Boolean
        get() = checks.all { it.success }

    val summary: String
        get() = checks.firstOrNull { it.failure }?.status ?: "Checks passed"

    // Factory methods / DSL

    fun custom(callback: CustomCheck.() -> Unit) = CustomCheck(this, callback)

    fun bundles(options: BundlesCheck.() -> Unit) = BundlesCheck(this).apply(options)

    fun components(options: ComponentsCheck.() -> Unit) = ComponentsCheck(this).apply(options)

    fun events(options: EventsCheck.() -> Unit) = EventsCheck(this).apply(options)

    fun timeout(options: TimeoutCheck.() -> Unit) = TimeoutCheck(this).apply(options)

    fun unavailable(options: UnavailableCheck.() -> Unit) = UnavailableCheck(this).apply(options)
}