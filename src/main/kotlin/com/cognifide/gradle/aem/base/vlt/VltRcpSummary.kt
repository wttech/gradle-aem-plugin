package com.cognifide.gradle.aem.base.vlt

import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.internal.Formats

data class VltRcpSummary(val source: Instance, val target: Instance, val copiedPaths: Long, val duration: Long) {

    val durationString: String
        get() = Formats.duration(duration)

    override fun toString(): String {
        return "VltRcpSummary(copiedPaths=$copiedPaths, duration=$durationString, source=$source, target=$target)"
    }

}