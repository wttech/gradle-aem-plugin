package com.cognifide.gradle.aem.common.instance.check

import com.cognifide.gradle.aem.common.instance.InstanceStatus
import com.cognifide.gradle.aem.common.instance.LocalInstance
import java.util.concurrent.TimeUnit

class UnavailableCheck(group: CheckGroup) : DefaultCheck(group) {

    /**
     * Local instances can be checked by running status script provided by AEM quickstart.
     * Determines when instance should be considered as unavailable.
     */
    var statusExpected = InstanceStatus.UNKNOWN

    /**
     * Status of remote instances cannot be checked easily. Because of that, check will work just a little bit longer.
     */
    var utilisationTime = TimeUnit.SECONDS.toMillis(15)

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
            val status = instance.status()
            if (status != InstanceStatus.UNKNOWN) {
                statusLogger.error(
                        "Awaiting not running",
                        "Incorrect instance status '$status'. Waiting for status '${InstanceStatus.UNKNOWN}' of $instance"
                )
            }
        } else {
            if (utilisationTime !in 0..stateTime) {
                statusLogger.error(
                        "Awaiting utilized",
                        "HTTP server not responding. Waiting for utilization (port releasing) of $instance"
                )
            }
        }
    }
}