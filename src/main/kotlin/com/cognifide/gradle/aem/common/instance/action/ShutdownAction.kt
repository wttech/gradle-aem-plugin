package com.cognifide.gradle.aem.common.instance.action

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.instance.names

class ShutdownAction(aem: AemExtension) : AbstractAction(aem) {

    override fun perform() {
        if (!enabled) {
            return
        }

        if (instances.isEmpty()) {
            aem.logger.info("No instances to shutdown.")
            return
        }

        shutdown()
    }

    private fun shutdown() {
        aem.logger.info("Awaiting instance(s) shutdown: ${instances.names}")

        // TODO reimplement shutdown action
    }
}