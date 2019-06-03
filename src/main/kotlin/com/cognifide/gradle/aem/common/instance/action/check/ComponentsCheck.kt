package com.cognifide.gradle.aem.common.instance.action.check

import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.action.CheckAction

@Suppress("MagicNumber")
class ComponentsCheck(action: CheckAction, instance: Instance) : DefaultCheck(action, instance) {

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
        val state = sync.osgiFramework.determineComponentState()
        if (state.unknown) {
            statusLogger.error("Unknown component state on $instance")
            return
        }

        val inactiveComponents = state.find(platformComponents, listOf()).filter { !it.active }
        if (inactiveComponents.isNotEmpty()) {
            statusLogger.error("Inactive components detected on $instance:\n${inactiveComponents.joinToString("\n")}")
        }

        val failedComponents = state.find(specificComponents, listOf()).filter { it.failedActivation }
        if (failedComponents.isNotEmpty()) {
            statusLogger.error("Components with failed activation detected on $instance:\n${failedComponents.joinToString("\n")}")
        }

        val unsatisfiedComponents = state.find(specificComponents, listOf()).filter { it.unsatisfied }
        if (unsatisfiedComponents.isNotEmpty()) {
            statusLogger.error("Unsatisfied components detected on $instance:\n${unsatisfiedComponents.joinToString("\n")}")
        }
    }
}