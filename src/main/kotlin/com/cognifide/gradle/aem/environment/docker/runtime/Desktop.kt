package com.cognifide.gradle.aem.environment.docker.runtime

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.utils.Formats
import org.gradle.internal.os.OperatingSystem

class Desktop(aem: AemExtension) : Base(aem) {

    override val name: String
        get() = NAME

    override val hostIp: String
        get() = aem.props.string("environment.docker.desktop.hostIp") ?: "127.0.0.1"

    override val safeVolumes: Boolean
        get() = !OperatingSystem.current().isWindows

    override fun determinePath(path: String) = Formats.normalizePath(path)

    companion object {
        const val NAME = "desktop"
    }
}
