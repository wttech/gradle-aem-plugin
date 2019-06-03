package com.cognifide.gradle.aem.common.instance.check

import com.cognifide.gradle.aem.common.utils.Formats

@Suppress("MagicNumber")
class ComponentsCheck(group: CheckGroup) : DefaultCheck(group) {

    var platformComponents = aem.props.list("instance.check.components.platform") ?: listOf(
            "com.day.crx.packaging.*",
            "org.apache.sling.installer.*"
    )

    var specificComponents = aem.props.list("instance.check.components.specific") ?: aem.javaPackages.map { "$it.*" }

    init {
        sync.apply {
            http.connectionTimeout = 10000
        }
    }

    @Suppress("ComplexMethod")
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

        val total = state.components.size

        val inactive = state.find(platformComponents, listOf()).filter { !it.active }
        if (inactive.isNotEmpty()) {
            statusLogger.error(
                    when (inactive.size) {
                        1 -> "Component inactive '${inactive.first().uid}'"
                        else -> "Components inactive (${Formats.percentExplained(inactive.size, total)})"
                    },
                    "Inactive components detected on $instance:\n${inactive.joinToString("\n")}"
            )
        }

        val failed = state.find(specificComponents, listOf()).filter { it.failedActivation }
        if (failed.isNotEmpty()) {
            statusLogger.error(
                    when (failed.size) {
                        1 -> "Component failed '${failed.first().uid}'"
                        else -> "Components failed (${Formats.percentExplained(failed.size, total)})"
                    },
                    "Components with failed activation detected on $instance:\n${failed.joinToString("\n")}"
            )
        }

        val unsatisfied = state.find(specificComponents, listOf()).filter { it.unsatisfied }
        if (unsatisfied.isNotEmpty()) {
            statusLogger.error(
                    when (unsatisfied.size) {
                        1 -> "Component unsatisfied '${unsatisfied.first().uid}'"
                        else -> "Components unsatisified (${Formats.percentExplained(unsatisfied.size, total)})"
                    },
                    "Unsatisfied components detected on $instance:\n${unsatisfied.joinToString("\n")}"
            )
        }
    }
}