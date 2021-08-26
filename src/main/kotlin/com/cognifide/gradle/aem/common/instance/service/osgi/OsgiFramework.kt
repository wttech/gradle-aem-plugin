package com.cognifide.gradle.aem.common.instance.service.osgi

import com.cognifide.gradle.aem.common.bundle.BundleFile
import com.cognifide.gradle.aem.common.instance.InstanceService
import com.cognifide.gradle.aem.common.instance.InstanceSync
import com.cognifide.gradle.common.CommonException
import com.cognifide.gradle.common.build.Retry
import com.cognifide.gradle.common.http.ResponseException
import com.cognifide.gradle.common.utils.Formats
import org.apache.http.HttpStatus
import java.io.File

/**
 * Controls OSGi framework using Apache Felix Web Console endpoints.
 *
 * @see <https://felix.apache.org/documentation/subprojects/apache-felix-web-console.html>
 */
@Suppress("TooManyFunctions")
class OsgiFramework(sync: InstanceSync) : InstanceService(sync) {

    // ----- Bundles -----

    /**
     * Get all OSGi bundles.
     */
    val bundles: List<Bundle> get() = determineBundleState().bundles

    /**
     * Determine all OSGi bundle states.
     */
    fun determineBundleState(): BundleState {
        logger.debug("Asking for OSGi bundles on $instance")

        return try {
            sync.http.get(BUNDLES_LIST_JSON) { asObjectFromJson(it, BundleState::class.java) }
        } catch (e: CommonException) {
            logger.debug("Cannot request OSGi bundles state on $instance", e)
            BundleState.unknown(e)
        }.apply {
            this.instance = this@OsgiFramework.instance
            this.bundles.forEach { it.instance = instance }
        }
    }

    /**
     * Find OSGi bundle by symbolic name.
     */
    fun findBundle(symbolicName: String): Bundle? = determineBundleState().bundles.find {
        symbolicName.equals(it.symbolicName, ignoreCase = true)
    }

    /**
     * Get OSGi bundle by symbolic name. Fail if not found.
     */
    fun getBundle(symbolicName: String): Bundle = findBundle(symbolicName)
            ?: throw OsgiException("OSGi bundle '$symbolicName' cannot be found on $instance.")

    /**
     * Start OSGi bundle. Does nothing if already started.
     */
    fun startBundle(symbolicName: String) {
        val bundle = getBundle(symbolicName)
        if (bundle.stable) {
            logger.info("Not starting already started OSGi $bundle on $instance.")
            return
        }

        logger.info("Starting OSGi $bundle on $instance.")
        sync.http.post("$BUNDLES_PATH/${bundle.id}", mapOf("action" to "start"))
    }

    fun startBundle(bundle: Bundle) = startBundle(bundle.symbolicName)

    /**
     * Stop OSGi bundle. Does nothing if already stopped.
     */
    fun stopBundle(symbolicName: String) {
        val bundle = getBundle(symbolicName)
        if (!bundle.stable) {
            logger.info("Not stopping already stopped OSGi $bundle on $instance.")
            return
        }

        logger.info("Stopping OSGi $bundle on $instance.")
        sync.http.post("$BUNDLES_PATH/${bundle.id}", mapOf("action" to "stop"))
    }

    fun stopBundle(bundle: Bundle) = stopBundle(bundle.symbolicName)

    fun <T> toggleBundle(symbolicName: String, action: () -> T): T = try {
        stopBundle(symbolicName)
        action()
    } finally {
        startBundle(symbolicName)
    }

    /**
     * Stop then start again OSGi bundle. Works correctly even it is already stopped.
     */
    fun restartBundle(symbolicName: String) {
        stopBundle(symbolicName)
        startBundle(symbolicName)
    }

    fun restartBundle(bundle: Bundle) = restartBundle(bundle.symbolicName)

    /**
     * Refresh OSGi bundle by symbolic name.
     */
    fun refreshBundle(symbolicName: String) {
        val bundle = getBundle(symbolicName)
        logger.info("Refreshing OSGi $bundle on $instance.")
        sync.http.post("$BUNDLES_PATH/${bundle.symbolicName}", mapOf("action" to "refresh"))
    }

    fun refreshBundle(bundle: Bundle) = refreshBundle(bundle.symbolicName)

    /**
     * Update OSGi bundle by symbolic name.
     */
    fun updateBundle(symbolicName: String) {
        val bundle = getBundle(symbolicName)
        logger.info("Updating OSGi $bundle on $instance.")
        sync.http.post("$BUNDLES_PATH/${bundle.symbolicName}", mapOf("action" to "update"))
    }

    fun updateBundle(bundle: Bundle) = updateBundle(bundle.symbolicName)

    /**
     * Install OSGi bundle JAR.
     */
    fun installBundle(
        bundle: File,
        start: Boolean = true,
        startLevel: Int = 20,
        refreshPackages: Boolean = true,
        retry: Retry = common.retry()
    ) {
        logger.info("Installing OSGi bundle '$bundle' on $instance.")

        retry.withCountdown<Unit, CommonException>("install bundle '${bundle.name}' on '${instance.name}'") {
            sync.http.postMultipart(BUNDLES_PATH, mapOf(
                    "action" to "install",
                    "bundlefile" to bundle,
                    "bundlestart" to "start".takeIf { start },
                    "bundlestartlevel" to startLevel,
                    "refreshPackages" to "refresh".takeIf { refreshPackages }
            )) { checkStatus(it, HttpStatus.SC_MOVED_TEMPORARILY) }
        }
    }

    /**
     * Uninstall OSGi bundle JAR.
     */
    fun uninstallBundle(bundle: File) {
        val bundleFile = BundleFile(bundle)
        logger.info("Uninstalling OSGi $bundleFile on $instance.")
        uninstallBundleInternal(bundleFile.symbolicName)
    }

    /**
     * Uninstall OSGi bundle by symbolic name.
     */
    fun uninstallBundle(symbolicName: String) {
        logger.info("Uninstalling OSGi bundle '$symbolicName' on $instance.")
        uninstallBundleInternal(symbolicName)
    }

    fun uninstallBundle(bundle: Bundle) = uninstallBundle(bundle.symbolicName)

    private fun uninstallBundleInternal(symbolicName: String) {
        sync.http.post("$BUNDLES_PATH/${getBundle(symbolicName).id}", mapOf("action" to "uninstall"))
    }

    // ----- Components -----

    /**
     * Get all OSGi components.
     */
    val components: List<Component> get() = determineComponentState().components

    /**
     * Determine OSGi components state.
     */
    fun determineComponentState(): ComponentState {
        logger.debug("Asking for OSGi components on $instance")

        return try {
            sync.http.get(COMPONENTS_LIST_JSON) { asObjectFromJson(it, ComponentState::class.java) }
        } catch (e: CommonException) {
            logger.debug("Cannot determine OSGi components state on $instance. Cause: ${e.message}", e)
            ComponentState.unknown()
        }.apply {
            this.instance = this@OsgiFramework.instance
            this.components.forEach { it.instance = instance }
        }
    }

    /**
     * Find OSGi component by PID.
     */
    fun findComponent(pid: String): Component? = determineComponentState().components.find {
        pid.equals(it.uid, ignoreCase = true)
    }

    /**
     * Get OSGi component by PID. Fail if not found.
     */
    fun getComponent(pid: String): Component = findComponent(pid)
            ?: throw OsgiException("OSGi component '$pid' cannot be found on $instance.")

    /**
     * Enable OSGi component. Does nothing if already enabled.
     */
    fun enableComponent(pid: String) {
        val component = getComponent(pid)
        if (component.id.isNotBlank()) {
            logger.info("Not enabling already enabled OSGi $component on $instance.")
            return
        }

        logger.info("Enabling OSGi $component on $instance.")
        sync.http.post("$COMPONENTS_PATH/${component.uid}", mapOf("action" to "enable"))
    }

    fun enableComponent(component: Component) = enableComponent(component.uid)

    /**
     * Disable OSGi component. Does nothing if already disabled.
     */
    fun disableComponent(pid: String) {
        val component = getComponent(pid)
        if (component.id.isBlank()) {
            logger.info("Not disabling already disabled OSGi $component on $instance.")
            return
        }

        logger.info("Disabling OSGi $component on $instance.")
        sync.http.post("$COMPONENTS_PATH/${component.id}", mapOf("action" to "disable"))
    }

    fun disableComponent(component: Component) = disableComponent(component.uid)

    /**
     * Disable then enable again OSGi component. Works correctly even it is already disabled.
     */
    fun restartComponent(pid: String) {
        disableComponent(pid)
        enableComponent(pid)
    }

    fun restartComponent(component: Component) = restartComponent(component.uid)

    fun <T> toggleComponent(pid: String, action: () -> T): T = try {
        disableComponent(pid)
        action()
    } finally {
        enableComponent(pid)
    }

    // ----- Configurations -----

    /**
     * Set properties for existing OSGi configuration.
     */
    fun configure(pid: String, propertyName: String, propertyValue: Any?) = configure(pid, mapOf(propertyName to propertyValue))

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
        logger.debug("Asking for OSGi configurations on $instance")

        return try {
            sync.http.get(CONFIGURATION_PATH) { response ->
                val html = asString(response)
                val configJson = CONFIGURATIONS_REGEX.find(html)?.groups?.get(1)?.value
                        ?: throw ResponseException("OSGi configuration cannot be found in console response of $instance.")
                Formats.toObjectFromJson<ConfigurationState>(configJson)
            }
        } catch (e: CommonException) {
            logger.debug("Cannot determine OSGi configuration state on $instance. Cause: ${e.message}", e)
            ConfigurationState.unknown()
        }.apply {
            this.instance = this@OsgiFramework.instance
            this.pids.forEach { it.instance = instance }
        }
    }

    /**
     * Find OSGi configuration by PID.
     * Also ensures that for given configuration there is metatype service available (to reduce typo errors).
     */
    fun findConfiguration(pid: String, metatypeChecking: Boolean = true): Configuration? = try {
        sync.http.get("$CONFIGURATION_PATH/$pid?post=true") { response ->
            asObjectFromJson(response, Configuration::class.java).takeIf { !metatypeChecking || !it.metatypeAbsence }
        }
    } catch (e: CommonException) {
        throw OsgiException("Cannot read OSGi configuration for PID '$pid' on $instance. Cause: ${e.message}", e)
    }?.apply {
        this.instance = this@OsgiFramework.instance
    }

    /**
     * Get OSGi configuration by PID. Fail if not found.
     */
    fun getConfiguration(pid: String): Configuration = findConfiguration(pid)
            ?: throw OsgiException("OSGi configuration for PID '$pid' cannot be found on $instance")

    /**
     * Get all OSGi configurations for specified factory PID.
     */
    fun getConfigurations(fpid: String): Sequence<Configuration> = determineConfigurationState().pids.asSequence()
            .filter { fpid == it.fpid }
            .mapNotNull { findConfiguration(it.id) }

    /**
     * Set properties for existing OSGi configuration.
     */
    fun updateConfiguration(pid: String, properties: Map<String, Any?>) = try {
        logger.info("Updating OSGi configuration for PID '$pid' on $instance using properties: $properties")

        val config = getConfiguration(pid)
        val props = configurationProperties(config, properties)

        sync.http.post("$CONFIGURATION_PATH/$pid", props) { checkStatus(it, HttpStatus.SC_MOVED_TEMPORARILY) }
    } catch (e: CommonException) {
        throw OsgiException("OSGi configuration for PID '$pid' cannot be updated on $instance. Cause: ${e.message}", e)
    }

    /**
     * Set properties for existing OSGi configuration.
     */
    fun updateConfiguration(pid: String, service: String, properties: Map<String, Any>) = updateConfiguration("$pid~$service", properties)

    /**
     * Set properties for existing OSGi configuration or create new.
     */
    fun saveConfiguration(pid: String, properties: Map<String, Any?>) = try {
        logger.info("Saving OSGi configuration for PID '$pid' on $instance using properties: $properties")

        val config = findConfiguration(pid, false)!! // endpoint always return data even for non-existing PID
        val props = configurationProperties(config, properties)

        sync.http.post("$CONFIGURATION_PATH/$pid", props) { checkStatus(it, HttpStatus.SC_MOVED_TEMPORARILY) }
    } catch (e: CommonException) {
        throw OsgiException("OSGi configuration for PID '$pid' cannot be saved on $instance. Cause: ${e.message}", e)
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
    fun deleteConfiguration(pid: String) = try {
        logger.info("Deleting OSGi configuration for PID '$pid' on $instance")

        val properties = mapOf(
                "apply" to 1,
                "delete" to 1
        )

        sync.http.post("$CONFIGURATION_PATH/$pid", properties)
    } catch (e: CommonException) {
        throw OsgiException("OSGi configuration for PID '$pid' cannot be deleted on $instance. Cause: ${e.message}", e)
    }

    fun deleteConfiguration(configuration: Configuration) = deleteConfiguration(configuration.pid)

    /**
     * Delete existing OSGi configuration.
     */
    fun deleteConfiguration(pid: String, service: String) = deleteConfiguration("$pid~$service")

    // ----- Events -----

    /**
     * Get OSGi events for current moment.
     */
    val events: List<Event> get() = determineEventState().events

    /**
     * Determine OSGi events for current moment.
     */
    fun determineEventState(): EventState {
        logger.debug("Asking for OSGi events on $instance")

        return try {
            sync.http.get(EVENTS_LIST_JSON) { asObjectFromJson(it, EventState::class.java) }
        } catch (e: CommonException) {
            logger.debug("Cannot determine OSGi events state on $instance. Cause: ${e.message}", e)
            EventState.unknown()
        }.apply {
            this.instance = this@OsgiFramework.instance
            this.events.forEach { it.instance = instance }
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

    private fun shutdown(type: String) = try {
        logger.info("Triggering OSGi framework shutdown of type '$type' on $instance.")
        sync.http.postUrlencoded(VMSTAT_PATH, mapOf("shutdown_type" to type))
    } catch (e: CommonException) {
        throw OsgiException("Cannot trigger shutdown of $instance. Cause: ${e.message}", e)
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
