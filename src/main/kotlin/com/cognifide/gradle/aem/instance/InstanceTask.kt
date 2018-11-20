package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.api.AemDefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal

open class InstanceTask : AemDefaultTask() {

    @Input
    var instanceHandles: List<LocalHandle> = listOf()

    @get:Internal
    val instances: List<LocalInstance>
        get() = instanceHandles.map { it.instance }

    override fun projectsEvaluated() {
        if (instanceHandles.isEmpty()) {
            instanceHandles = aem.instanceHandles
        }
    }

}