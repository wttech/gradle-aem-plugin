package com.cognifide.gradle.aem.common.instance.service.workflow

import com.cognifide.gradle.aem.common.instance.service.repository.Node
import com.cognifide.gradle.aem.common.instance.service.workflow.ToggleStrategy.Companion.ENABLED_PROP

class Toggle61 : ToggleStrategy {

    override fun toggle(launcherNode: Node, state: Boolean) {
        launcherNode.saveProperty(ENABLED_PROP, state)
    }
}