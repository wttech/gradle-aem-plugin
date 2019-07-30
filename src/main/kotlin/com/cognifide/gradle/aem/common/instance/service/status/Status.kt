package com.cognifide.gradle.aem.common.instance.service.status

import com.cognifide.gradle.aem.AemException
import com.cognifide.gradle.aem.common.http.RequestException
import com.cognifide.gradle.aem.common.instance.InstanceService
import com.cognifide.gradle.aem.common.instance.InstanceSync
import com.cognifide.gradle.aem.common.utils.Formats
import java.util.*

/**
 * Allows to read statuses available at Apache Felix Web Console.
 *
 * @see <https://felix.apache.org/documentation/subprojects/apache-felix-web-console.html>
 */
class Status(sync: InstanceSync) : InstanceService(sync) {

    /**
     * System properties of instance read once across whole build, fail-safe.
     */
    val systemProperties: Map<String, String>
        get() {
            if (!aem.instanceOptions.statusProperties) {
                return mapOf()
            }

            return aem.buildScope.tryGetOrPut("${instance.httpUrl}$SYSTEM_PROPERTIES_PATH") {
                try {
                    readSystemProperties().apply {
                        aem.logger.info("Successfully read system properties of $instance")
                    }
                } catch (e: AemException) {
                    aem.logger.debug("Cannot read system properties of $instance", e)
                    null
                }
            } ?: mapOf()
        }

    /**
     * Read system properties like server timezone & encoding, Java version, OS details.
     */
    @Suppress("unchecked_cast")
    fun readSystemProperties(): Map<String, String> = sync.http.get(SYSTEM_PROPERTIES_PATH) {
        Properties().apply { load(useSystemPropertiesAsIni(asString(it))) } as Map<String, String>
    }

    /**
     * System properties endpoint response is broken / not valid INI file, because is not escaping
     * Windows paths with backslash. Below code is fixing 'Malformed \uxxxx encoding.' exception.
     */
    private fun useSystemPropertiesAsIni(text: String) = text.replace("\\", "\\\\").byteInputStream()

    /**
     * Instance version read once across whole build, fail-safe.
     */
    val productVersion: String
        get() {
            if (!aem.instanceOptions.statusProperties) {
                return PRODUCT_VERSION_UNKNOWN
            }

            return aem.buildScope.tryGetOrPut("${instance.httpUrl}$PRODUCT_INFO_PATH") {
                try {
                    readProductVersion().apply {
                        aem.logger.info("Successfully read product version '$this' of $instance")
                    }
                } catch (e: AemException) {
                    aem.logger.debug("Cannot read product info of $instance")
                    null
                }
            } ?: PRODUCT_VERSION_UNKNOWN
        }

    /**
     * Read AEM version of instance.
     */
    fun readProductVersion(): String {
        val text = try {
            sync.http.get(PRODUCT_INFO_PATH) { asString(it) }
        } catch (e: RequestException) {
            throw StatusException("Cannot request product version of $instance. Cause: ${e.message}", e)
        }

        val version = text.lineSequence().mapNotNull {
            PRODUCT_VERSION_REGEX.matchEntire(it)?.groupValues?.get(1)
        }.firstOrNull()

        return version ?: throw StatusException("Cannot find product version in response of $instance:\n$text")
    }

    companion object {

        const val SYSTEM_PROPERTIES_PATH = "/system/console/status-System%20Properties.txt"

        const val PRODUCT_INFO_PATH = "/system/console/status-productinfo.txt"

        val PRODUCT_VERSION_REGEX = Regex("^ {2}Adobe Experience Manager \\((.*)\\)$")

        val PRODUCT_VERSION_UNKNOWN = Formats.VERSION_UNKNOWN.version
    }
}