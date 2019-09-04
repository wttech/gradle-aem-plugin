package com.cognifide.gradle.aem.common.instance.service.workflow

class Workflow(val manager: WorkflowManager, val id: String) {

    private val repository = manager.repository

    private val instance = manager.instance

    private val logger = manager.aem.logger

    val launcher = repository.node(when {
            manager.configFrozen -> "/conf/global/settings/workflow/launcher/config/$id"
            else -> "/etc/workflow/launcher/config/$id"
        })

    val launcherFrozen = repository.node("/libs/settings/workflow/launcher/config/$id")

    val exists: Boolean
        get() = launcher.exists || (manager.configFrozen && launcherFrozen.exists)

    val enabled: Boolean
        get() = launcher.properties.boolean(ENABLED_PROP) ?: false

    private var toggleInitial: Boolean? = null

    fun toggle(flag: Boolean) {
        if (manager.configFrozen && !launcher.exists) {
            logger.info("Copying workflow launcher from '${launcherFrozen.path}' to ${launcher.path} on $instance")
            launcher.copyFrom(launcherFrozen.path)
        }

        if (flag) {
            if (enabled == flag) {
                logger.info("Enabling workflow launcher '${launcher.path}' not needed on $instance")
                return
            } else {
                logger.info("Enabling workflow launcher '${launcher.path}' on $instance")
            }
        } else {
            if (enabled == flag) {
                logger.info("Disabling workflow launcher '${launcher.path}' not needed on $instance")
                return
            } else {
                logger.info("Disabling workflow launcher '${launcher.path}' on $instance")
            }
        }

        toggleInitial = enabled
        launcher.apply { saveProperty(ENABLED_PROP, flag); reload() }
    }

    fun restore() {
        if (toggleInitial != null) {
            toggle(toggleInitial!!)
        }
    }

    fun enable() = toggle(true)

    fun disable() = toggle(false)

    companion object {

        const val ENABLED_PROP = "enabled"
    }
}
