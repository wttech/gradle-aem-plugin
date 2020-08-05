package com.cognifide.gradle.aem.common.instance.service.status

import com.cognifide.gradle.aem.common.instance.InstanceService
import com.cognifide.gradle.aem.common.instance.InstanceSync
import com.cognifide.gradle.common.CommonException
import com.cognifide.gradle.common.http.RequestException
import com.cognifide.gradle.common.utils.Formats
import com.cognifide.gradle.common.utils.Patterns
import java.util.*

/**
 * Allows to read statuses available at Apache Felix Web Console.
 *
 * @see <https://felix.apache.org/documentation/subprojects/apache-felix-web-console.html>
 */
class Status(sync: InstanceSync) : InstanceService(sync) {

    /**
     * Check if instance was available at least once across whole build, fail-safe.
     */
    val available: Boolean get() = systemProperties.isNotEmpty()

    /**
     * On-demand checks instance availability.
     */
    fun checkAvailable(): Boolean = try {
        readProperties(SYSTEM_PROPERTIES_PATH).isNotEmpty()
    } catch (e: CommonException) {
        logger.debug("Seems that instance is not available: $instance", e)
        false
    }

    /**
     * System properties of instance read once across whole build, fail-safe.
     */
    val systemProperties: Map<String, String> get() = readPropertiesOnce(SYSTEM_PROPERTIES_PATH)

    /**
     * Sling setting of instance read once across whole build, fail-safe.
     */
    val slingSettings: Map<String, String> get() = readPropertiesOnce(SLING_SETTINGS_PATH)

    /**
     * Sling properties of instance read once across whole build, fail-safe.
     */
    val slingProperties: Map<String, String> get() = readPropertiesOnce(SLING_PROPERTIES_PATH)

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

    fun readPropertiesOnce(path: String): Map<String, String> {
        if (aem.commonOptions.offline.get()) {
            return mapOf()
        }

        return common.buildScope.tryGetOrPut("${instance.httpUrl}$path") {
            try {
                readProperties(path).apply {
                    aem.logger.info("Successfully read status properties at path '$path' on $instance.")
                }
            } catch (e: StatusException) {
                aem.logger.debug("Failed when reading status properties at path '$path' on $instance!", e)
                null
            }
        } ?: mapOf()
    }

    /**
     * Read system properties like server timezone & encoding, Java version, OS details.
     */
    @Suppress("unchecked_cast")
    fun readProperties(path: String): Map<String, String> = try {
        sync.http.get(path) {
            Properties().apply { load(statusPropertiesAsIni(asString(it))) } as Map<String, String>
        }
    } catch (e: CommonException) {
        throw StatusException("Cannot read status properties for path '$path' on $instance! Cause: ${e.message}", e)
    }

    /**
     * Status properties endpoints response is not valid INI file, because is not escaping
     * Windows paths with backslash. Below code is fixing 'Malformed \uxxxx encoding.' exception.
     */
    private fun statusPropertiesAsIni(text: String) = text.lineSequence()
            .filter { Patterns.wildcard(it, "* = *") } // Filter headings starting with '*** '
            .map {
                val key = it.substringBefore("=").trim().replace(" ", "_") // Spaces in keys fix
                val value = it.substringAfter("=").trim().replace("\\", "\\\\") // Windows paths fix
                "$key=$value"
            }
            .joinToString("\n")
            .byteInputStream()

    /**
     * Instance version read once across whole build, fail-safe.
     */
    val productVersion: String
        get() {
            if (aem.commonOptions.offline.get()) {
                return PRODUCT_VERSION_UNKNOWN
            }

            return common.buildScope.tryGetOrPut("${instance.httpUrl}$PRODUCT_INFO_PATH") {
                try {
                    readProductVersion().apply {
                        aem.logger.info("Successfully read product version '$this' of $instance")
                    }
                } catch (e: CommonException) {
                    aem.logger.debug("Cannot read product info of $instance")
                    null
                }
            } ?: PRODUCT_VERSION_UNKNOWN
        }

    companion object {

        const val SYSTEM_PROPERTIES_PATH = "/system/console/status-System Properties.txt"

        const val SLING_SETTINGS_PATH = "/system/console/status-slingsettings.txt"

        const val SLING_PROPERTIES_PATH = "/system/console/status-slingprops.txt"

        const val PRODUCT_INFO_PATH = "/system/console/status-productinfo.txt"

        val PRODUCT_VERSION_REGEX = Regex("^ {2}Adobe Experience Manager \\((.*)\\)$")

        val PRODUCT_VERSION_UNKNOWN = Formats.versionUnknown().version
    }
}
