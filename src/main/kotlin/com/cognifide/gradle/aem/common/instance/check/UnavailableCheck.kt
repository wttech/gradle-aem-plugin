package com.cognifide.gradle.aem.common.instance.check

import com.cognifide.gradle.aem.common.instance.LocalInstance
import com.cognifide.gradle.aem.common.instance.local.Status
import java.util.concurrent.TimeUnit

class UnavailableCheck(group: CheckGroup) : DefaultCheck(group) {

    /**
     * Status of remote instances cannot be checked easily. Because of that, check will work just a little bit longer.
     */
    val utilisationTime = aem.obj.long { convention(TimeUnit.SECONDS.toMillis(15)) }

    override fun check() {
        if (instance.available) {
            val bundleState = state(sync.osgiFramework.determineBundleState())
            if (!bundleState.unknown) {
                statusLogger.error(
                    "Bundles stable (${bundleState.stablePercent})",
                    "Bundles stable (${bundleState.stablePercent}). HTTP server still responding on $instance"
                )
                return
            }
        } else {
            val reachableStatus = state(sync.status.checkReachableStatus())
            if (reachableStatus >= 0) {
                statusLogger.error(
                    "Reachable ($reachableStatus)",
                    "Reachable - status code ($reachableStatus). HTTP server still responding on $instance"
                )
                return
            }
        }

        if (instance is LocalInstance) {
            val status = state(instance.checkStatus())
            if (!status.runnable) {
                statusLogger.error(
                    "Awaiting not running",
                    "Unexpected instance status '$status'. Waiting for status '${Status.Type.RUNNABLE.map { it.name }}' of $instance"
                )
            }
        } else {
            if (utilisationTime.get() !in 0..progress.stateTime) {
                statusLogger.error(
                    "Awaiting utilized",
                    "HTTP server not responding. Waiting for utilization (port releasing) of $instance"
                )
            }
        }
    }
}
