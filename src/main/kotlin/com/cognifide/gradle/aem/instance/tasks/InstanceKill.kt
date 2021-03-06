package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.LocalInstance
import org.gradle.api.tasks.TaskAction

open class InstanceKill : LocalInstance() {

    @TaskAction
    fun kill() {
        localInstanceManager.base.examinePrerequisites(anyInstances)

        val killedInstances = localInstanceManager.kill(anyInstances)
        if (killedInstances.isNotEmpty()) {
            common.notifier.lifecycle("Instance(s) killed", "Which: ${killedInstances.names}")
        }
    }

    init {
        description = "Kill local AEM instance process(es)"
    }

    companion object {
        const val NAME = "instanceKill"
    }
}
