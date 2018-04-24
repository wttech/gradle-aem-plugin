package com.cognifide.gradle.aem.instance.action

import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.instance.InstanceSync
import org.gradle.api.Project

/**
 * Reloads all instances and waits until all be stable.
 */
class ReloadAction(project: Project, instances: List<Instance>) : AwaitAction(project, instances) {

    var delay = config.reloadDelay

    private fun reload() {
        instances.parallelStream().forEach { InstanceSync.create(project, it).reload(delay) }
    }

    override fun perform() {
        reload()
        super.perform()
    }

}