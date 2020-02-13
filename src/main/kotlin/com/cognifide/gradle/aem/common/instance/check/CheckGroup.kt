package com.cognifide.gradle.aem.common.instance.check

import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.common.build.CollectingLogger
import org.apache.commons.lang3.builder.HashCodeBuilder

class CheckGroup(
    val runner: CheckRunner,
    val instance: Instance,
    checkFactory: CheckFactory.() -> List<Check>
) {

    val stateBuilder = HashCodeBuilder()

    val statusLogger = CollectingLogger()

    var checks: List<Check> = CheckFactory(this).run(checkFactory)

    @Suppress("TooGenericExceptionCaught", "LoopWithTooManyJumpStatements")
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
        get() = checks.firstOrNull { it.failure }?.status ?: "Passed"
}
