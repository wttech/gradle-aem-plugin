package com.cognifide.gradle.aem.common.pkg.vault

import com.cognifide.gradle.common.utils.Formats
import java.io.File

data class VaultSummary(val command: String, val contentDir: File, val duration: Long) {

    val durationString: String
        get() = Formats.duration(duration)

    override fun toString(): String {
        return "VaultSummary(command=$command, contentDir=$contentDir, duration=$durationString)"
    }
}
