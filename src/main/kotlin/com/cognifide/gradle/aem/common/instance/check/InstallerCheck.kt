package com.cognifide.gradle.aem.common.instance.check

@Suppress("MagicNumber")
class InstallerCheck(group: CheckGroup) : DefaultCheck(group) {

    override fun check() {
        logger.info("Checking OSGi installer on $instance")

        val state = state(sync.jmx.determineSlingOsgiInstallerState())
        if (state.unknown) {
            statusLogger.error(
                    "Installer state unknown",
                    "Unknown Sling OSGi Installer state on $instance"
            )
            return
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
        }

        if (sync.repository.slingInstallerPaused) {
            statusLogger.error(
                    "Installation paused",
                    "Sling OSGi Installer is paused on $instance"
            )
        }
    }
}
