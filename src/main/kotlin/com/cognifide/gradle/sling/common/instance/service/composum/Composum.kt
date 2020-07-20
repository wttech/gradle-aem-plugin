package com.cognifide.gradle.sling.common.instance.service.composum

import com.cognifide.gradle.sling.common.instance.InstanceService
import com.cognifide.gradle.sling.common.instance.InstanceSync
import com.cognifide.gradle.common.CommonException
import com.cognifide.gradle.common.http.RequestException

/**
 * Allows to communicate with Composum endpoints.
 *
 */
class Composum(sync: InstanceSync) : InstanceService(sync) {

    /**
     * Node types read once across whole build, fail-safe.
     */
    val nodeTypes: String
        get() {
            if (sling.commonOptions.offline.get()) {
                return NODE_TYPES_UNKNOWN
            }

            return common.buildScope.tryGetOrPut("${instance.httpUrl}$EXPORT_NODE_TYPE_PATH") {
                try {
                    readNodeTypes().apply {
                        sling.logger.info("Successfully read node types of $instance")
                    }
                } catch (e: CommonException) {
                    sling.logger.debug("Cannot read node types of $instance")
                    null
                }
            } ?: NODE_TYPES_UNKNOWN
        }

    /**
     * Read repository node types using CRX DE endpoint.
     *
     * TODO https://github.com/ist-dresden/composum/issues/199
     */
    fun readNodeTypes(): String = try {
        // sync.http.get("/bin/cpm/nodetypes") { asString(it) }
        throw ComposumException("Reading node types is not yet supported by Composum!")
    } catch (e: RequestException) {
        throw ComposumException("Cannot request node types of $instance. Cause: ${e.message}", e)
    }

    companion object {

        const val EXPORT_NODE_TYPE_PATH = "/bin/cpm/nodetypes"

        const val NODE_TYPES_UNKNOWN = ""
    }
}
