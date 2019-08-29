package com.cognifide.gradle.aem.common.instance.service.workflow

import com.cognifide.gradle.aem.common.instance.service.repository.Node

class Workflow(val manager: WorkflowManager, val id: String) {

    private val repository = manager.repository

    private val instance = manager.instance

    private val logger = manager.aem.logger

    val launcher: Node
        get() = repository.node(when {
            manager.configFrozen -> "/conf/global/settings/workflow/launcher/config/$id"
            else -> "/etc/workflow/launcher/config/$id"
        })

    val launcherFrozen: Node
        get() = when {
            manager.configFrozen -> repository.node("/libs/settings/workflow/launcher/config/$id")
            else -> throw WorkflowException("Workflow launcher frozen is not available on $instance!")
        }

    val exists: Boolean
        get() = launcher.exists || (manager.configFrozen && launcherFrozen.exists)

    fun toggle(flag: Boolean) {
        if (manager.configFrozen && !launcher.exists) {
            logger.info("Copying workflow launcher from '${launcherFrozen.path}' to ${launcher.path} on $instance")
            launcher.copyFrom(launcherFrozen.path)
        }

        if (flag) {
            logger.info("Enabling workflow launcher '${launcher.path}' on $instance")
        } else {
            logger.info("Disabling workflow launcher '${launcher.path}' on $instance")
        }

        launcher.saveProperty("enabled", flag)
    }

    fun enable() = toggle(true)

    fun disable() = toggle(false)
}