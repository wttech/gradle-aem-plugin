package com.cognifide.gradle.aem.instance.action

import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.instance.InstanceSync
import org.gradle.api.Project

class ReloadAction(project: Project, instances: List<Instance>) : AwaitAction(project, instances) {

    var delay = config.reloadDelay

    private fun reload() {
        instances.parallelStream().forEach { InstanceSync(project, it).reload(delay) }
    }

    override fun perform() {
        reload()
        super.perform()
    }

}