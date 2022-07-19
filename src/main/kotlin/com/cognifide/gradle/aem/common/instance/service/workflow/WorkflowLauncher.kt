package com.cognifide.gradle.aem.common.instance.service.workflow

class WorkflowLauncher(val manager: WorkflowManager, val id: String) {

    val instance = manager.instance

    private val repository = manager.repository

    private val logger = manager.aem.logger

    val node = repository.node(
        when {
            manager.instance.version.frozen -> "/conf/global/settings/workflow/launcher/config/$id"
            else -> "/etc/workflow/launcher/config/$id"
        }
    )

    val nodeFrozen = repository.node("/libs/settings/workflow/launcher/config/$id")

    val exists: Boolean
        get() = node.exists || (manager.instance.version.frozen && nodeFrozen.exists)

    val enabled: Boolean
        get() = node.properties.boolean(ENABLED_PROP) ?: false

    private var toggleInitial: Boolean? = null

    internal var toggleIntended: Boolean? = null

    val scheduler by lazy { WorkflowScheduler(this) }

    fun toggle() {
        toggleIntended?.let { toggle(it) }
    }

    fun toggle(flag: Boolean) {
        if (manager.instance.version.frozen && !node.exists) {
            logger.info("Copying workflow launcher from '${nodeFrozen.path}' to ${node.path} on $instance")
            node.copyFrom(nodeFrozen.path)
        }

        if (flag) {
            if (enabled == flag) {
                logger.info("Enabling workflow launcher '${node.path}' not needed on $instance")
                return
            } else {
                logger.info("Enabling workflow launcher '${node.path}' on $instance")
            }
        } else {
            if (enabled == flag) {
                logger.info("Disabling workflow launcher '${node.path}' not needed on $instance")
                return
            } else {
                logger.info("Disabling workflow launcher '${node.path}' on $instance")
            }
        }

        toggleInitial = enabled
        node.apply { saveProperty(ENABLED_PROP, flag); reload() }
    }

    fun restore() {
        when {
            manager.restoreIntended.get() && toggleIntended != null -> toggle(!toggleIntended!!)
            toggleInitial != null -> toggle(toggleInitial!!)
        }
    }

    fun enable() = toggle(true)

    fun disable() = toggle(false)

    override fun toString(): String = "Workflow(id='$id', instance=$instance)"

    companion object {

        const val ENABLED_PROP = "enabled"
    }
}
