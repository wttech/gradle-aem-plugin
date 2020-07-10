package com.cognifide.gradle.sling.instance.tasks

import com.cognifide.gradle.sling.common.instance.names
import com.cognifide.gradle.sling.common.tasks.LocalInstanceTask
import org.gradle.api.tasks.TaskAction

open class InstanceKill : LocalInstanceTask() {

    @TaskAction
    fun kill() {
        localInstanceManager.base.examinePrerequisites(instances.get())

        val killedInstances = localInstanceManager.kill(instances.get())
        if (killedInstances.isNotEmpty()) {
            common.notifier.lifecycle("Instance(s) killed", "Which: ${killedInstances.names}")
        }
    }

    init {
        description = "Kill local Sling instance process(es)"
    }

    companion object {
        const val NAME = "instanceKill"
    }
}
