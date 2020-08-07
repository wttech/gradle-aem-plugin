package com.cognifide.gradle.aem.common.instance.service.jmx

import com.cognifide.gradle.aem.common.instance.InstanceService
import com.cognifide.gradle.aem.common.instance.InstanceSync
import com.cognifide.gradle.common.CommonException
import com.fasterxml.jackson.databind.JsonNode

/**
 * Reads JMX data provided by Sling framework through resource provider.
 *
 * TODO rename to 'SlingInstaller' (move methods from 'Repository' here)
 */
class Jmx(sync: InstanceSync) : InstanceService(sync) {

    fun determineSlingOsgiInstallerState(): SlingOsgiInstallerState = try {
        logger.debug("Determining Sling OSGi Installer state on $instance")
        readSlingOsgiInstallerState()
    } catch (e: CommonException) {
        logger.debug("Cannot request Sling OSGi Installer state on $instance", e)
        SlingOsgiInstallerState.unknown(instance)
    }

    fun readSlingOsgiInstallerState(): SlingOsgiInstallerState = try {
        sync.http.get("$MBEANS_ROOT/$OSGI_INSTALLER_PATH") { asObjectFromJson<SlingOsgiInstallerState>(it) }
    } catch (e: CommonException) {
        throw JmxException("Cannot read Sling OSGi Installer state!", e)
    }.apply { this.instance = this@Jmx.instance }

    fun read(mbeanPath: String): JsonNode {
        val mbeanFullPath = "$MBEANS_ROOT/$mbeanPath"
        return try {
            sync.http.get(mbeanFullPath) { asJson(it) }
        } catch (e: CommonException) {
            throw JmxException("Cannot read JMX using path '$mbeanFullPath' on $instance!")
        }
    }

    companion object {
        const val MBEANS_ROOT = "/system/sling/monitoring/mbeans"

        const val OSGI_INSTALLER_PATH = "org/apache/sling/installer/Installer/Sling OSGi Installer.json"
    }
}
