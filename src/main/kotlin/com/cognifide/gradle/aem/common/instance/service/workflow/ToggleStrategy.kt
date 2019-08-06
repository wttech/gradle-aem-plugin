package com.cognifide.gradle.aem.common.instance.service.workflow

import com.cognifide.gradle.aem.common.instance.service.repository.Node

interface ToggleStrategy {

    fun toggle(launcherNode: Node, expected: Boolean)

    fun changeRequired(launcher: Node, expected: Boolean): Boolean

    companion object {
        const val ENABLED_PROP = "enabled"
    }
}