package com.cognifide.gradle.aem.common.tasks

import com.cognifide.gradle.aem.AemDefaultTask
import com.cognifide.gradle.aem.common.instance.LocalInstance
import com.cognifide.gradle.aem.common.instance.LocalInstanceManager
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal

open class LocalInstanceTask : AemDefaultTask() {

    @Input
    var instances: List<LocalInstance> = listOf()

    @get:Internal
    val manager: LocalInstanceManager
        get() = aem.localInstanceManager

    override fun projectsEvaluated() {
        if (instances.isEmpty()) {
            instances = aem.localInstances
        }
    }
}