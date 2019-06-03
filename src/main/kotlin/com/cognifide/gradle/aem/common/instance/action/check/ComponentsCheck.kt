package com.cognifide.gradle.aem.common.instance.action.check

@Suppress("MagicNumber")
class ComponentsCheck(group: CheckGroup) : DefaultCheck(group) {

    var platformComponents = setOf(
            "com.day.crx.packaging.*",
            "org.apache.sling.installer.*"
    )

    init {
        sync.apply {
            http.connectionTimeout = 10000
        }
    }

    var specificComponents = aem.javaPackages.map { "$it.*" }

    override fun check() {
        aem.logger.info("Checking OSGi components on $instance")

        val state = sync.osgiFramework.determineComponentState()
        if (state.unknown) {
            statusLogger.error(
                    "Components unknown",
                    "Unknown component state on $instance"
            )
            return
        }

        val inactive = state.find(platformComponents, listOf()).filter { !it.active }
        if (inactive.isNotEmpty()) {
            statusLogger.error(
                    "Components inactive (${inactive.size})",
                    "Inactive components detected on $instance:\n${inactive.joinToString("\n")}"
            )
        }

        val failed = state.find(specificComponents, listOf()).filter { it.failedActivation }
        if (failed.isNotEmpty()) {
            statusLogger.error(
                    "Components failed (${failed.size})",
                    "Components with failed activation detected on $instance:\n${failed.joinToString("\n")}"
            )
        }

        val unsatisfied = state.find(specificComponents, listOf()).filter { it.unsatisfied }
        if (unsatisfied.isNotEmpty()) {
            statusLogger.error(
                    "Components unsatisfied (${unsatisfied.size})",
                    "Unsatisfied components detected on $instance:\n${unsatisfied.joinToString("\n")}"
            )
        }
    }
}