package com.cognifide.gradle.aem.common.instance.service.groovy

import com.cognifide.gradle.aem.common.utils.Formats

data class GroovyEvalSummary(
    val statuses: List<GroovyEvalStatus>,
    val duration: Long
) {
    val total: Int
        get() = statuses.size

    val failed: Int
        get() = statuses.count { it.fail }

    val succeeded: Int
        get() = statuses.count { it.success }

    val succeededPercent
        get() = Formats.percentExplained(succeeded, total)

    val durationString: String
        get() = Formats.duration(duration)

    override fun toString(): String {
        return "${javaClass.simpleName}(successes=$succeeded, failed=$failed, total=$total, duration=$durationString)"
    }

    companion object {
        fun empty() = GroovyEvalSummary(listOf(), 0L)
    }
}
