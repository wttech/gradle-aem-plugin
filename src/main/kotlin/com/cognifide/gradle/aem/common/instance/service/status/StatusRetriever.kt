package com.cognifide.gradle.aem.common.instance.service.status

import com.cognifide.gradle.aem.AemException
import com.cognifide.gradle.aem.common.instance.InstanceService
import com.cognifide.gradle.aem.common.instance.InstanceSync
import java.util.*

/**
 * Allows to read statuses available at Apache Felix Web Console.
 *
 * @see <https://felix.apache.org/documentation/subprojects/apache-felix-web-console.html>
 */
class StatusRetriever(sync: InstanceSync) : InstanceService(sync) {

    /**
     * System properties of instance. Read once across whole build, fail-safe.
     */
    val systemProperties: Map<String, String> = aem.buildScope.getOrPut("${instance.httpUrl}$SYSTEM_PROPERTIES_PATH") {
        when {
            aem.instanceOptions.systemProperties -> try {
                readSystemProperties()
            } catch (e: AemException) {
                aem.logger.warn("Cannot read system properties of $instance")
                aem.logger.debug("Instance system properties read error", e)

                mapOf<String, String>()
            }
            else -> mapOf()
        }
    }

    /**
     * Read system properties like server timezone & encoding, Java version, OS details.
     */
    @Suppress("unchecked_cast")
    fun readSystemProperties(): Map<String, String> = sync.http.get(SYSTEM_PROPERTIES_PATH) {
        Properties().apply { load(fixSystemPropertiesIniFormat(asString(it))) } as Map<String, String>
    }

    /**
     * System properties endpoint response is broken / not valid INI file, because is not escaping
     * Windows paths with backslash. Below code is fixing 'Malformed \uxxxx encoding.' exception.
     */
    private fun fixSystemPropertiesIniFormat(text: String) = text.replace("\\", "\\\\").byteInputStream()

    companion object {

        const val SYSTEM_PROPERTIES_PATH = "/system/console/status-System%20Properties.txt"
    }
}