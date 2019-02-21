package com.cognifide.gradle.aem.common.tasks

import com.cognifide.gradle.aem.common.AemDefaultTask
import com.cognifide.gradle.aem.instance.Instance
import org.gradle.api.tasks.Input

open class InstanceTask : AemDefaultTask() {

    @Input
    var instances: List<Instance> = listOf()

    override fun projectsEvaluated() {
        if (instances.isEmpty()) {
            instances = aem.instances
        }
    }
}