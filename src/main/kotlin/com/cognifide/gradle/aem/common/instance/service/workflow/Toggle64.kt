package com.cognifide.gradle.aem.common.instance.service.workflow

import com.cognifide.gradle.aem.AemConstants.WF_LAUNCHER_PATH_6_4_LIBS
import com.cognifide.gradle.aem.common.instance.service.repository.Node
import com.cognifide.gradle.aem.common.instance.service.workflow.ToggleStrategy.Companion.ENABLED_PROP
import org.apache.commons.lang3.StringUtils

class Toggle64 : ToggleStrategy {

    override fun toggle(launcher: Node, expected: Boolean) {
        if (!launcher.exists) {
            val workflowName = StringUtils.substringAfterLast(launcher.path, "/")
            val otherLauncher = launcher.repository.node("$WF_LAUNCHER_PATH_6_4_LIBS$workflowName")
            otherLauncher.copy(launcher.path)
            launcher.reload()
        }
        launcher.saveProperty(ENABLED_PROP, expected)
    }

    override fun changeRequired(launcher: Node, expected: Boolean): Boolean {
        var state = true
        if (!launcher.exists) {
            // todo fallback to /libs to get the property (get the repository object somehow)
        } else {
            state = launcher.properties[ENABLED_PROP] as Boolean
        }
        return expected != state
    }
}