package com.cognifide.gradle.aem.common.instance.service.crx

import com.cognifide.gradle.aem.common.instance.InstanceService
import com.cognifide.gradle.aem.common.instance.InstanceSync
import com.cognifide.gradle.common.CommonException
import com.cognifide.gradle.common.http.RequestException

/**
 * Allows to communicate with CRX DE endpoints.
 *
 * @see <https://helpx.adobe.com/pl/experience-manager/6-5/sites/developing/using/developing-with-crxde-lite.html>
 */
class Crx(sync: InstanceSync) : InstanceService(sync) {

    /**
     * Node types read once across whole build, fail-safe.
     */
    val nodeTypes: String
        get() {
            if (aem.commonOptions.offline.get()) {
                return NODE_TYPES_UNKNOWN
            }

            return common.buildScope.tryGetOrPut("${instance.httpUrl}$EXPORT_NODE_TYPE_PATH") {
                try {
                    readNodeTypes().apply {
                        aem.logger.info("Successfully read CRX node types of $instance")
                    }
                } catch (e: CommonException) {
                    aem.logger.debug("Cannot read CRX node types of $instance")
                    null
                }
            } ?: NODE_TYPES_UNKNOWN
        }

    /**
     * Read repository node types using CRX DE endpoint.
     */
    fun readNodeTypes(): String = try {
        sync.http.get(EXPORT_NODE_TYPE_PATH) { asString(it) }
    } catch (e: RequestException) {
        throw CrxException("Cannot request node types of $instance. Cause: ${e.message}", e)
    }

    companion object {

        const val NODE_TYPES_UNKNOWN = ""

        const val EXPORT_NODE_TYPE_PATH = "/crx/de/exportnodetype.jsp"
    }
}
