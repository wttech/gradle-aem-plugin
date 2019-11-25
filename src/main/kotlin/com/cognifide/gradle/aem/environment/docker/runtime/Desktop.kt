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

    override val hostInternalIp: String?
        get() = when {
            OperatingSystem.current().isWindows || OperatingSystem.current().isMacOsX -> null
            else -> detectHostInternalIp() ?: aem.props.string("environment.docker.desktop.hostInternalIp") ?: "172.17.0.1"
        }

    /**
     * TODO implement automatic detection of IP 'host.docker.internal' for Unix OS other than Mac OSX e.g Ubuntu
     *
     * @see <https://nickjanetakis.com/blog/docker-tip-65-get-your-docker-hosts-ip-address-from-in-a-container>
     */
    private fun detectHostInternalIp(): String? = null

    companion object {
        const val NAME = "desktop"
    }
}
