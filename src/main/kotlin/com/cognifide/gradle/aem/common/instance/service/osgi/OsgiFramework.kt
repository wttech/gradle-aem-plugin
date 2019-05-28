package com.cognifide.gradle.aem.common.instance.service.osgi

import com.cognifide.gradle.aem.AemException
import com.cognifide.gradle.aem.common.instance.InstanceException
import com.cognifide.gradle.aem.common.instance.InstanceService
import com.cognifide.gradle.aem.common.instance.InstanceSync

/**
 * Controls OSGi framework using Apache Felix Web Console endpoints.
 *
 * @see <https://felix.apache.org/documentation/subprojects/apache-felix-web-console.html>
 */
class OsgiFramework(sync: InstanceSync) : InstanceService(sync) {

    fun determineBundleState(): BundleState {
        aem.logger.debug("Asking for OSGi bundles on $instance")

        return try {
            sync.http.get(BUNDLES_LIST_JSON) { asObjectFromJson(it, BundleState::class.java) }
        } catch (e: AemException) {
            aem.logger.debug("Cannot request OSGi bundles state on $instance", e)
            BundleState.unknown(e)
        }
    }

    fun findBundle(symbolicName: String): Bundle? {
        return determineBundleState().bundles.find {
            symbolicName.equals(it.symbolicName, ignoreCase = true)
        }
    }

    fun getBundle(symbolicName: String): Bundle {
        return findBundle(symbolicName)
                ?: throw InstanceException("OSGi bundle '$symbolicName' cannot be found on $instance.")
    }

    fun startBundle(symbolicName: String) {
        val bundle = getBundle(symbolicName)
        if (bundle.stable) {
            aem.logger.info("Not starting already started OSGi $bundle on $instance.")
            return
        }

        aem.logger.info("Starting OSGi $bundle on $instance.")
        sync.http.post("$BUNDLES_PATH/${bundle.id}", mapOf("action" to "start"))
    }

    fun stopBundle(symbolicName: String) {
        val bundle = getBundle(symbolicName)
        if (!bundle.stable) {
            aem.logger.info("Not stopping already stopped OSGi $bundle on $instance.")
            return
        }

        aem.logger.info("Stopping OSGi $bundle on $instance.")
        sync.http.post("$BUNDLES_PATH/${bundle.id}", mapOf("action" to "stop"))
    }

    fun restartBundle(symbolicName: String) {
        stopBundle(symbolicName)
        startBundle(symbolicName)
    }

    fun refreshBundle(symbolicName: String) {
        val bundle = getBundle(symbolicName)
        aem.logger.info("Refreshing OSGi $bundle on $instance.")
        sync.http.post("$BUNDLES_PATH/${bundle.symbolicName}", mapOf("action" to "refresh"))
    }

    fun updateBundle(symbolicName: String) {
        val bundle = getBundle(symbolicName)
        aem.logger.info("Updating OSGi $bundle on $instance.")
        sync.http.post("$BUNDLES_PATH/${bundle.symbolicName}", mapOf("action" to "update"))
    }

    fun determineComponentState(): ComponentState {
        aem.logger.debug("Asking for OSGi components on $instance")

        return try {
            sync.http.get(COMPONENTS_LIST_JSON) { asObjectFromJson(it, ComponentState::class.java) }
        } catch (e: AemException) {
            aem.logger.debug("Cannot determine OSGi components state on $instance", e)
            ComponentState.unknown()
        }
    }

    fun determineEventState(): EventState {
        aem.logger.debug("Asking for OSGi events on $instance")

        return try {
            sync.http.get(EVENTS_LIST_JSON) { asObjectFromJson(it, EventState::class.java) }
        } catch (e: AemException) {
            aem.logger.debug("Cannot determine OSGi events state on $instance", e)
            EventState.unknown()
        }
    }

    fun findComponent(pid: String): Component? {
        return determineComponentState().components.find {
            pid.equals(it.uid, ignoreCase = true)
        }
    }

    fun getComponent(pid: String): Component {
        return findComponent(pid) ?: throw InstanceException("OSGi component '$pid' cannot be found on $instance.")
    }

    fun enableComponent(pid: String) {
        val component = getComponent(pid)
        if (component.id.isNotBlank()) {
            aem.logger.info("Not enabling already enabled OSGi $component on $instance.")
            return
        }

        aem.logger.info("Enabling OSGi $component on $instance.")
        sync.http.post("$COMPONENTS_PATH/${component.uid}", mapOf("action" to "enable"))
    }

    fun disableComponent(pid: String) {
        val component = getComponent(pid)
        if (component.id.isBlank()) {
            aem.logger.info("Not disabling already disabled OSGi $component on $instance.")
            return
        }

        aem.logger.info("Disabling OSGi $component on $instance.")
        sync.http.post("$COMPONENTS_PATH/${component.id}", mapOf("action" to "disable"))
    }

    fun restartComponent(pid: String) {
        disableComponent(pid)
        enableComponent(pid)
    }

    fun restart() = shutdown("Restart")

    fun stop() = shutdown("Stop")

    private fun shutdown(type: String) {
        try {
            aem.logger.info("Triggering OSGi framework shutdown on $instance.")
            sync.http.postUrlencoded(VMSTAT_PATH, mapOf("shutdown_type" to type))
        } catch (e: AemException) {
            throw InstanceException("Cannot trigger shutdown of $instance.", e)
        }
    }

    companion object {
        const val BUNDLES_PATH = "/system/console/bundles"

        const val BUNDLES_LIST_JSON = "$BUNDLES_PATH.json"

        const val COMPONENTS_PATH = "/system/console/components"

        const val COMPONENTS_LIST_JSON = "$COMPONENTS_PATH.json"

        const val EVENTS_LIST_JSON = "/system/console/events.json"

        const val VMSTAT_PATH = "/system/console/vmstat"
    }
}