package com.cognifide.gradle.aem.common.instance.service.osgi

import com.cognifide.gradle.aem.AemException
import com.cognifide.gradle.aem.common.http.ResponseException
import com.cognifide.gradle.aem.common.instance.InstanceException
import com.cognifide.gradle.aem.common.instance.InstanceService
import com.cognifide.gradle.aem.common.instance.InstanceSync
import com.cognifide.gradle.aem.common.utils.Formats
import org.apache.http.HttpStatus

/**
 * Controls OSGi framework using Apache Felix Web Console endpoints.
 *
 * @see <https://felix.apache.org/documentation/subprojects/apache-felix-web-console.html>
 */
class OsgiFramework(sync: InstanceSync) : InstanceService(sync) {

    // ----- Bundles -----

    /**
     * Get all OSGi bundles.
     */
    val bundles: List<Bundle>
        get() = determineBundleState().bundles

    /**
     * Determine all OSGi bundle states.
     */
    fun determineBundleState(): BundleState {
        aem.logger.debug("Asking for OSGi bundles on $instance")

        return try {
            sync.http.get(BUNDLES_LIST_JSON) { asObjectFromJson(it, BundleState::class.java) }
        } catch (e: AemException) {
            aem.logger.debug("Cannot request OSGi bundles state on $instance", e)
            BundleState.unknown(e)
        }
    }

    /**
     * Find OSGi bundle by symbolic name.
     */
    fun findBundle(symbolicName: String): Bundle? {
        return determineBundleState().bundles.find {
            symbolicName.equals(it.symbolicName, ignoreCase = true)
        }
    }

    /**
     * Get OSGi bundle by symbolic name. Fail if not found.
     */
    fun getBundle(symbolicName: String): Bundle {
        return findBundle(symbolicName)
                ?: throw InstanceException("OSGi bundle '$symbolicName' cannot be found on $instance.")
    }

    /**
     * Start OSGi bundle. Does nothing if already started.
     */
    fun startBundle(symbolicName: String) {
        val bundle = getBundle(symbolicName)
        if (bundle.stable) {
            aem.logger.info("Not starting already started OSGi $bundle on $instance.")
            return
        }

        aem.logger.info("Starting OSGi $bundle on $instance.")
        sync.http.post("$BUNDLES_PATH/${bundle.id}", mapOf("action" to "start"))
    }

    /**
     * Stop OSGi bundle. Does nothing if already stopped.
     */
    fun stopBundle(symbolicName: String) {
        val bundle = getBundle(symbolicName)
        if (!bundle.stable) {
            aem.logger.info("Not stopping already stopped OSGi $bundle on $instance.")
            return
        }

        aem.logger.info("Stopping OSGi $bundle on $instance.")
        sync.http.post("$BUNDLES_PATH/${bundle.id}", mapOf("action" to "stop"))
    }

    /**
     * Stop then start again OSGi bundle. Works correctly even it is already stopped.
     */
    fun restartBundle(symbolicName: String) {
        stopBundle(symbolicName)
        startBundle(symbolicName)
    }

    /**
     * Refresh OSGi bundle by symbolic name.
     */
    fun refreshBundle(symbolicName: String) {
        val bundle = getBundle(symbolicName)
        aem.logger.info("Refreshing OSGi $bundle on $instance.")
        sync.http.post("$BUNDLES_PATH/${bundle.symbolicName}", mapOf("action" to "refresh"))
    }

    /**
     * Update OSGi bundle by symbolic name.
     */
    fun updateBundle(symbolicName: String) {
        val bundle = getBundle(symbolicName)
        aem.logger.info("Updating OSGi $bundle on $instance.")
        sync.http.post("$BUNDLES_PATH/${bundle.symbolicName}", mapOf("action" to "update"))
    }

    // ----- Components -----

    /**
     * Get all OSGi components.
     */
    val components: List<Component>
        get() = determineComponentState().components

    /**
     * Determine OSGi components state.
     */
    fun determineComponentState(): ComponentState {
        aem.logger.debug("Asking for OSGi components on $instance")

        return try {
            sync.http.get(COMPONENTS_LIST_JSON) { asObjectFromJson(it, ComponentState::class.java) }
        } catch (e: AemException) {
            aem.logger.debug("Cannot determine OSGi components state on $instance. Cause: ${e.message}", e)
            ComponentState.unknown()
        }
    }

    /**
     * Find OSGi component by PID.
     */
    fun findComponent(pid: String): Component? {
        return determineComponentState().components.find {
            pid.equals(it.uid, ignoreCase = true)
        }
    }

    /**
     * Get OSGi component by PID. Fail if not found.
     */
    fun getComponent(pid: String): Component {
        return findComponent(pid) ?: throw InstanceException("OSGi component '$pid' cannot be found on $instance.")
    }

    /**
     * Enable OSGi component. Does nothing if already enabled.
     */
    fun enableComponent(pid: String) {
        val component = getComponent(pid)
        if (component.id.isNotBlank()) {
            aem.logger.info("Not enabling already enabled OSGi $component on $instance.")
            return
        }

        aem.logger.info("Enabling OSGi $component on $instance.")
        sync.http.post("$COMPONENTS_PATH/${component.uid}", mapOf("action" to "enable"))
    }

    /**
     * Disable OSGi component. Does nothing if already disabled.
     */
    fun disableComponent(pid: String) {
        val component = getComponent(pid)
        if (component.id.isBlank()) {
            aem.logger.info("Not disabling already disabled OSGi $component on $instance.")
            return
        }

        aem.logger.info("Disabling OSGi $component on $instance.")
        sync.http.post("$COMPONENTS_PATH/${component.id}", mapOf("action" to "disable"))
    }

    /**
     * Disable then enable again OSGi component. Works correctly even it is already disabled.
     */
    fun restartComponent(pid: String) {
        disableComponent(pid)
        enableComponent(pid)
    }

    // ----- Configurations -----

    /**
     * Set properties for existing OSGi configuration.
     */
    fun configure(pid: String, properties: Map<String, Any?>) = updateConfiguration(pid, properties)

    /**
     * Get all OSGi configurations.
     */
    val configurations: Sequence<Configuration>
        get() = determineConfigurationState().pids.asSequence()
                .mapNotNull { findConfiguration(it.id) }

    /**
     * Determine all OSGi configuration PIDs.
     */
    fun determineConfigurationState(): ConfigurationState {
        aem.logger.debug("Asking for OSGi configurations on $instance")

        return try {
            sync.http.get(CONFIGURATION_PATH) { response ->
                val html = asString(response)
                val configJson = CONFIGURATIONS_REGEX.find(html)?.groups?.get(1)?.value
                        ?: throw ResponseException("OSGi configuration cannot be found in console response of $instance.")
                Formats.fromJson(configJson, ConfigurationState::class.java)
            }
        } catch (e: AemException) {
            aem.logger.debug("Cannot determine OSGi configuration state on $instance. Cause: ${e.message}", e)
            ConfigurationState.unknown()
        }
    }

    /**
     * Find OSGi configuration by PID.
     * Also ensures that for given configuration there is metatype service available (to reduce typo errors).
     */
    fun findConfiguration(pid: String, metatypeChecking: Boolean = true): Configuration? {
        return try {
            sync.http.get("$CONFIGURATION_PATH/$pid?post=true") { response ->
                asObjectFromJson(response, Configuration::class.java).takeIf { !metatypeChecking || !it.metatypeAbsence }
            }
        } catch (e: AemException) {
            throw InstanceException("Cannot read OSGi configuration for PID '$pid' on $instance. Cause: ${e.message}", e)
        }
    }

    /**
     * Get OSGi configuration by PID. Fail if not found.
     */
    fun getConfiguration(pid: String): Configuration {
        return findConfiguration(pid)
                ?: throw InstanceException("OSGi configuration for PID '$pid' cannot be found on $instance")
    }

    /**
     * Get all OSGi configurations for specified factory PID.
     */
    fun getConfigurations(fpid: String): Sequence<Configuration> {
        return determineConfigurationState().pids.asSequence()
                .filter { fpid == it.fpid }
                .mapNotNull { findConfiguration(it.id) }
    }

    /**
     * Set properties for existing OSGi configuration.
     */
    fun updateConfiguration(pid: String, properties: Map<String, Any?>) {
        try {
            aem.logger.info("Updating OSGi configuration for PID '$pid' on $instance using properties: $properties")

            val config = getConfiguration(pid)
            val props = configurationProperties(config, properties)

            sync.http.post("$CONFIGURATION_PATH/$pid", props) { checkStatus(it, HttpStatus.SC_MOVED_TEMPORARILY) }
        } catch (e: AemException) {
            throw InstanceException("OSGi configuration for PID '$pid' cannot be updated on $instance. Cause: ${e.message}", e)
        }
    }

    /**
     * Set properties for existing OSGi configuration.
     */
    fun updateConfiguration(pid: String, service: String, properties: Map<String, Any>) = updateConfiguration("$pid~$service", properties)

    /**
     * Set properties for existing OSGi configuration or create new.
     */
    fun saveConfiguration(pid: String, properties: Map<String, Any?>) {
        try {
            aem.logger.info("Saving OSGi configuration for PID '$pid' on $instance using properties: $properties")

            val config = findConfiguration(pid, false)!! // endpoint always return data even for non-existing PID
            val props = configurationProperties(config, properties)

            sync.http.post("$CONFIGURATION_PATH/$pid", props) { checkStatus(it, HttpStatus.SC_MOVED_TEMPORARILY) }
        } catch (e: AemException) {
            throw InstanceException("OSGi configuration for PID '$pid' cannot be saved on $instance. Cause: ${e.message}", e)
        }
    }

    private fun configurationProperties(existingConfig: Configuration, properties: Map<String, Any?>): Map<String, Any?> {
        val valueProperties = existingConfig.properties + properties
        return valueProperties + mapOf(
                "apply" to true,
                "action" to "ajaxConfigManager",
                "\$location" to existingConfig.bundleLocation,
                "propertylist" to valueProperties.keys.joinToString(",")
        )
    }

    /**
     * Set properties for existing OSGi configuration or create new.
     */
    fun saveConfiguration(pid: String, service: String, properties: Map<String, Any>) = saveConfiguration("$pid~$service", properties)

    /**
     * Delete existing OSGi configuration.
     */
    fun deleteConfiguration(pid: String) {
        try {
            aem.logger.info("Deleting OSGi configuration for PID '$pid' on $instance")

            val properties = mapOf(
                    "apply" to 1,
                    "delete" to 1
            )

            sync.http.post("$CONFIGURATION_PATH/$pid", properties)
        } catch (e: AemException) {
            throw InstanceException("OSGi configuration for PID '$pid' cannot be deleted on $instance. Cause: ${e.message}", e)
        }
    }

    /**
     * Delete existing OSGi configuration.
     */
    fun deleteConfiguration(pid: String, service: String) = deleteConfiguration("$pid~$service")

    // ----- Events -----

    /**
     * Get OSGi events for current moment.
     */
    val events: List<Event>
        get() = determineEventState().events

    /**
     * Determine OSGi events for current moment.
     */
    fun determineEventState(): EventState {
        aem.logger.debug("Asking for OSGi events on $instance")

        return try {
            sync.http.get(EVENTS_LIST_JSON) { asObjectFromJson(it, EventState::class.java) }
        } catch (e: AemException) {
            aem.logger.debug("Cannot determine OSGi events state on $instance. Cause: ${e.message}", e)
            EventState.unknown()
        }
    }

    // ----- Framework -----

    /**
     * Restart OSGi framework (Apache Felix)
     */
    fun restart() = shutdown("Restart")

    /**
     * Stop OSGi framework (Apache Felix)
     *
     * Warning! After executing instance will be no longer available.
     */
    fun stop() = shutdown("Stop")

    private fun shutdown(type: String) {
        try {
            aem.logger.info("Triggering OSGi framework shutdown on $instance.")
            sync.http.postUrlencoded(VMSTAT_PATH, mapOf("shutdown_type" to type))
        } catch (e: AemException) {
            throw InstanceException("Cannot trigger shutdown of $instance. Cause: ${e.message}", e)
        }
    }

    companion object {
        const val BUNDLES_PATH = "/system/console/bundles"

        const val BUNDLES_LIST_JSON = "$BUNDLES_PATH.json"

        const val COMPONENTS_PATH = "/system/console/components"

        const val COMPONENTS_LIST_JSON = "$COMPONENTS_PATH.json"

        const val EVENTS_LIST_JSON = "/system/console/events.json"

        const val VMSTAT_PATH = "/system/console/vmstat"

        const val CONFIGURATION_PATH = "/system/console/configMgr"

        val CONFIGURATIONS_REGEX = Regex("configData = (.*);")
    }
}
