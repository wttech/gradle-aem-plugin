package com.cognifide.gradle.aem.environment.docker.runtime

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.utils.Formats
import com.cognifide.gradle.aem.environment.docker.DockerProcess
import org.gradle.internal.os.OperatingSystem

class Desktop(aem: AemExtension) : Base(aem) {

    override val name: String
        get() = NAME

    override val hostIp: String
        get() = aem.props.string("environment.docker.desktop.hostIp") ?: "127.0.0.1"

    override val safeVolumes: Boolean
        get() = !OperatingSystem.current().isWindows

    override fun determinePath(path: String) = Formats.normalizePath(path)

    override val hostInternalIp: String?
        get() = when {
            OperatingSystem.current().isWindows || OperatingSystem.current().isMacOsX -> null
            else -> detectHostInternalIp() ?: aem.props.string("environment.docker.desktop.hostInternalIp") ?: "172.17.0.1"
        }

    @Suppress("SpreadOperator", "TooGenericException")
    private fun detectHostInternalIp(): String? = try {
        DockerProcess.execString {
            val args = listOf("run", "alpine", "/bin/ash", "-c", "ip -4 route list match 0/0 | cut -d ' ' -f 3")
            withArgs(*args.toTypedArray())
        }.takeIf { it.isNotBlank() }
    } catch (e: Exception) {
        logger.debug("Cannot detect Docker host internal API. Cause: ${e.message}")
        null
    }

    companion object {
        const val NAME = "desktop"
    }
}
