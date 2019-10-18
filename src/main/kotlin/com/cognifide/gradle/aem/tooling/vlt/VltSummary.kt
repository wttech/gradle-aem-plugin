package com.cognifide.gradle.aem.tooling.vlt

import com.cognifide.gradle.aem.common.utils.Formats
import java.io.File

data class VltSummary(val command: String, val contentDir: File, val duration: Long) {

    val durationString: String
        get() = Formats.duration(duration, false)

    override fun toString(): String {
        return "VltSummary(command=$command, contentDir=$contentDir, duration=$durationString)"
    }
}
