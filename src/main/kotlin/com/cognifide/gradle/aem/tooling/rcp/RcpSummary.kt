package com.cognifide.gradle.aem.tooling.rcp

import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.utils.Formats

data class RcpSummary(
    val source: Instance,
    val target: Instance,
    val copiedPaths: Long,
    val duration: Long
) {
    val durationString: String
        get() = Formats.duration(duration, false)

    override fun toString(): String {
        return "RcpSummary(copiedPaths=$copiedPaths, duration=$durationString, source=$source, target=$target)"
    }
}
