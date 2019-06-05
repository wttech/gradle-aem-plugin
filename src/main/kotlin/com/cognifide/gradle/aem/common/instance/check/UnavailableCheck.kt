package com.cognifide.gradle.aem.common.instance.check

import com.cognifide.gradle.aem.common.instance.LocalInstance
import java.util.concurrent.TimeUnit

class UnavailableCheck(group: CheckGroup) : DefaultCheck(group) {

    /**
     * Intermittently local instance cannot be down because control port is not cleaned up by instance.
     *
     * As a workaround, when instance is not responding and same state is maintained for some longer time,
     * then this file is force-deleted.
     */
    var controlPortAge = TimeUnit.SECONDS.toMillis(45)

    override fun check() {
        val bundleState = state(sync.osgiFramework.determineBundleState())
        if (!bundleState.unknown) {
            statusLogger.error(
                    "Bundles stable (${bundleState.stablePercent})",
                    "Bundles stable (${bundleState.stablePercent}). HTTP server still responding on $instance"
            )
            return
        }

        if (instance is LocalInstance) {
            if (controlPortAge in 0..stateTime) {
                instance.controlPortFile.delete()
            }

            if (instance.pidFile.exists()) {
                statusLogger.error(
                        "PID file exists",
                        "PID file still exists for $instance"
                )
            }

            if (instance.controlPortFile.exists()) {
                statusLogger.error(
                        "Control port exists",
                        "Control port still exists for $instance"
                )
            }
        }
    }
}