package com.cognifide.gradle.aem.common.instance.service.groovy

import com.cognifide.gradle.aem.common.utils.Formats

data class GroovyScriptSummary(
        val statuses: List<GroovyScriptStatus>,
        val duration: Long
) {
    val total: Int
        get() = statuses.size

    val failed: Int
        get() = statuses.count { it.success }

    val succeeded: Int
        get() = total - failed

    val succeededPercent
        get() = Formats.percentExplained(succeeded, total)

    val durationString: String
        get() = Formats.duration(duration)

    override fun toString(): String {
        return "GroovyScriptSummary(successes=$succeeded, failed=$failed, total=$total, duration=$durationString)"
    }
}