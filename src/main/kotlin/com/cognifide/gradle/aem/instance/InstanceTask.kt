package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.common.AemDefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal

open class InstanceTask : AemDefaultTask() {

    @Input
    var localHandles: List<LocalHandle> = listOf()

    @get:Internal
    val instances: List<LocalInstance>
        get() = localHandles.map { it.instance }

    override fun projectsEvaluated() {
        if (localHandles.isEmpty()) {
            localHandles = aem.localHandles
        }
    }
}