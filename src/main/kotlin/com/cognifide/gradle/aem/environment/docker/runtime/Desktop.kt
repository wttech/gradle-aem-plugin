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

    override val definedHostInternal: Boolean
        get() = OperatingSystem.current().isWindows

    override fun determinePath(path: String) = Formats.normalizePath(path)

    val hostInternalIp: String
        get() = aem.props.string("environment.docker.desktop.hostInternalIp") ?: "172.17.0.1"

    companion object {
        const val NAME = "desktop"
    }
}
