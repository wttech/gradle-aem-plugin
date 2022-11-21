package com.cognifide.gradle.aem.common.instance.service.sling

import com.cognifide.gradle.aem.common.instance.InstanceService
import com.cognifide.gradle.aem.common.instance.InstanceSync
import com.cognifide.gradle.aem.common.instance.service.repository.Repository
import com.cognifide.gradle.aem.common.instance.service.repository.RepositoryException
import com.cognifide.gradle.common.CommonException

/**
 * Monitors the Sling Installer state.
 */
class SlingInstaller(sync: InstanceSync) : InstanceService(sync) {

    val paused: Boolean
        get() = try {
            checkPause()
        } catch (e: RepositoryException) {
            logger.debug("Repository error", e)
            false
        }

    fun checkPause(): Boolean = try {
        sync.repository.node(Repository.SLING_INSTALLER_PAUSE).children().any()
    } catch (e: CommonException) {
        throw RepositoryException("Cannot check Sling OSGi installer pause on $instance!", e)
    }

    val state: SlingInstallerState
        get() = try {
            logger.debug("Determining Sling OSGi Installer state on $instance")
            readState()
        } catch (e: SlingException) {
            logger.debug("Cannot request Sling OSGi Installer state on $instance", e)
            SlingInstallerState.unknown(instance)
        }

    fun readState(): SlingInstallerState = try {
        sync.http.get("$MBEANS_ROOT/$OSGI_INSTALLER_PATH") { asObjectFromJson<SlingInstallerState>(it) }
    } catch (e: CommonException) {
        throw SlingException("Cannot read Sling OSGi Installer state!", e)
    }.apply { this.instance = this@SlingInstaller.instance }

    companion object {
        const val MBEANS_ROOT = "/system/sling/monitoring/mbeans"

        const val OSGI_INSTALLER_PATH = "org/apache/sling/installer/Installer/Sling OSGi Installer.json"
    }
}
