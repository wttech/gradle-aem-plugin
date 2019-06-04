package com.cognifide.gradle.aem.common.instance.check

import com.cognifide.gradle.aem.common.instance.LocalInstance
import java.util.concurrent.TimeUnit

class UnavailableCheck(group: CheckGroup) : DefaultCheck(group) {

    var controlPortAge = TimeUnit.MINUTES.toMillis(1)

    override fun check() {
        val bundleState = sync.osgiFramework.determineBundleState()

        val stillResponding = !bundleState.unknown
        if (stillResponding) {
            statusLogger.error(
                    "Bundles stable (${bundleState.stablePercent})",
                    "HTTP server still responding on $instance"
            )
            return
        }

        if (instance is LocalInstance) {
            if (controlPortAge >= 0 && runner.runningTime > controlPortAge) {
                instance.controlPortFile.delete()
            }

            val hasControlPort = instance.controlPortFile.exists()
            if (hasControlPort) {
                statusLogger.error(
                        "Control port exists",
                        "Control port still exists for $instance"
                )
            }
        }
    }
}