package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.AemDefaultTask
import com.cognifide.gradle.aem.instance.LocalInstance
import com.cognifide.gradle.aem.instance.LocalInstanceOptions
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal

open class Instance : AemDefaultTask() {

    @Input
    var instances: List<LocalInstance> = listOf()

    @get:Internal
    val instanceOptions: LocalInstanceOptions
        get() = aem.config.localInstanceOptions

    override fun projectsEvaluated() {
        if (instances.isEmpty()) {
            instances = aem.localInstances
        }
    }
}