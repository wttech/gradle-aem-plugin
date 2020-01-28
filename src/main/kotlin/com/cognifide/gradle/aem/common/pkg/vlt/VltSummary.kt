package com.cognifide.gradle.aem.common.pkg.vlt

import com.cognifide.gradle.common.utils.Formats
import java.io.File

data class VltSummary(val command: String, val contentDir: File, val duration: Long) {

    val durationString: String
        get() = Formats.duration(duration)

    override fun toString(): String {
        return "VltSummary(command=$command, contentDir=$contentDir, duration=$durationString)"
    }
}
