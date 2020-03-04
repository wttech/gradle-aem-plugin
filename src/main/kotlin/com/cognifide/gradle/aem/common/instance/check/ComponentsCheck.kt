package com.cognifide.gradle.aem.common.instance.check

import com.cognifide.gradle.aem.common.utils.shortenClass
import com.cognifide.gradle.common.utils.Formats

@Suppress("MagicNumber")
class ComponentsCheck(group: CheckGroup) : DefaultCheck(group) {

    val platformComponents = aem.obj.strings {
        convention(listOf(
                "com.day.crx.packaging.*",
                "org.apache.sling.installer.*"
        ))
    }

    val specificComponents = aem.obj.strings { convention(listOf()) }

    init {
        sync.apply {
            http.connectionTimeout.convention(10_000)
        }
    }

    @Suppress("ComplexMethod")
    override fun check() {
        aem.logger.info("Checking OSGi components on $instance")

        val state = state(sync.osgiFramework.determineComponentState())
        if (state.unknown) {
            statusLogger.error(
                    "Components unknown",
                    "Unknown component state on $instance"
            )
            return
        }

        val total = state.components.size

        val inactive = state.find(platformComponents.get(), listOf()).filter { !it.active }
        if (inactive.isNotEmpty()) {
            statusLogger.error(
                    when (inactive.size) {
                        1 -> "Component inactive '${inactive.first().uid.shortenClass()}'"
                        in 2..10 -> "Components inactive (${inactive.size})"
                        else -> "Components inactive (${Formats.percentExplained(inactive.size, total)})"
                    },
                    "Inactive components detected on $instance:\n${logValues(inactive)}"
            )
        }

        val failed = state.find(specificComponents.get(), listOf()).filter { it.failedActivation }
        if (failed.isNotEmpty()) {
            statusLogger.error(
                    when (failed.size) {
                        1 -> "Component failed '${failed.first().uid.shortenClass()}'"
                        in 2..10 -> "Components failed (${failed.size})"
                        else -> "Components failed (${Formats.percentExplained(failed.size, total)})"
                    },
                    "Components with failed activation detected on $instance:\n${logValues(failed)}"
            )
        }

        val unsatisfied = state.find(specificComponents.get(), listOf()).filter { it.unsatisfied }
        if (unsatisfied.isNotEmpty()) {
            statusLogger.error(
                    when (unsatisfied.size) {
                        1 -> "Component unsatisfied '${unsatisfied.first().uid.shortenClass()}'"
                        in 2..10 -> "Components unsatisfied (${unsatisfied.size})"
                        else -> "Components unsatisified (${Formats.percentExplained(unsatisfied.size, total)})"
                    },
                    "Unsatisfied components detected on $instance:\n${logValues(unsatisfied)}"
            )
        }
    }
}
