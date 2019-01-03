package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.AemDefaultTask
import com.cognifide.gradle.aem.instance.LocalHandle
import com.cognifide.gradle.aem.instance.LocalInstance
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal

open class Instance : AemDefaultTask() {

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