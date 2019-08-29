package com.cognifide.gradle.aem.common.instance.service.workflow

import com.cognifide.gradle.aem.AemException
import com.cognifide.gradle.aem.common.instance.service.repository.Node

class Workflow(val manager: WorkflowManager, val id: String) {

    private val repository = manager.repository

    private val launcherNode: Node
        get() = repository.node(when {
            manager.configFrozen -> "/conf/global/settings/workflow/launcher/config/$id"
            else -> "/etc/workflow/launcher/config/$id"
        })

    private val launcherFrozenNode: Node
        get() = when {
            manager.configFrozen -> repository.node("/libs/settings/workflow/launcher/config/$id")
            else -> throw AemException("Workflow launcher frozen node is not available!")
        }

    val exists: Boolean
        get() = launcherNode.exists || (manager.configFrozen && launcherFrozenNode.exists)

    fun toggle(state: Boolean) {
        if (manager.configFrozen && !launcherNode.exists) {
            launcherNode.copyFrom(launcherFrozenNode.path)
        }

        launcherNode.saveProperty("enabled", state)
    }
}