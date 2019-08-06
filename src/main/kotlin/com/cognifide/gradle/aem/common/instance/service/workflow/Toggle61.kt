package com.cognifide.gradle.aem.common.instance.service.workflow

import com.cognifide.gradle.aem.common.instance.service.repository.Node
import com.cognifide.gradle.aem.common.instance.service.workflow.ToggleStrategy.Companion.ENABLED_PROP

class Toggle61 : ToggleStrategy {

    override fun toggle(launcherNode: Node, expected: Boolean) {
        launcherNode.saveProperty(ENABLED_PROP, expected)
    }

    override fun changeRequired(launcher: Node, expected: Boolean): Boolean {
        return expected != launcher.properties[ENABLED_PROP] ?: true
    }
}