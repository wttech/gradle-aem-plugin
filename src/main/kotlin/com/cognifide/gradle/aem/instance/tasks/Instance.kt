package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.AemDefaultTask
import com.cognifide.gradle.aem.instance.LocalInstance
import org.gradle.api.tasks.Input

open class Instance : AemDefaultTask() {

    @Input
    var instances: List<LocalInstance> = listOf()

    override fun projectsEvaluated() {
        if (instances.isEmpty()) {
            instances = aem.localInstances
        }
    }
}