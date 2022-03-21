package com.cognifide.gradle.aem.common.instance.check

class InstallerCheck(group: CheckGroup) : DefaultCheck(group) {

    val busy = aem.obj.boolean { convention(true) }

    val pause = aem.obj.boolean { convention(true) }

    override fun check() {
        if (busy.get()) {
            if (isBusy()) return
        }

        if (pause.get()) {
            if (isPaused()) return
        }
    }

    private fun isBusy(): Boolean {
        logger.info("Checking OSGi installer busyness on $instance")

        val state = state(sync.slingInstaller.state)
        if (state.unknown) {
            statusLogger.error(
                "Installer state unknown",
                "Unknown Sling OSGi Installer state on $instance"
            )
            return true
        }

        if (state.busy) {
            if (state.activeResourceCount > 0) {
                statusLogger.error(
                    "Installing resources (${state.activeResourceCount})",
                    "Sling OSGi Installer is processing resources (${state.activeResourceCount})' on $instance"
                )
            } else {
                statusLogger.error(
                    "Installation in progress",
                    "Sling OSGi Installer is active on $instance"
                )
            }
            return true
        }

        return false
    }

    private fun isPaused(): Boolean {
        logger.info("Checking OSGi installer pause on $instance")

        val paused = state(sync.slingInstaller.paused)
        if (paused) {
            statusLogger.error(
                "Installation paused",
                "Sling OSGi Installer is paused on $instance"
            )
            return true
        }

        return false
    }
}
