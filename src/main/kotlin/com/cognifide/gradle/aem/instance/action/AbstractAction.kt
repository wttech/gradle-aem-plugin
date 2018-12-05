package com.cognifide.gradle.aem.instance.action

import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.instance.InstanceAction
import com.cognifide.gradle.aem.instance.LocalHandle
import com.cognifide.gradle.aem.instance.toLocalHandles
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal

abstract class AbstractAction(
    @Internal
    @Transient
    val aem: AemExtension
) : InstanceAction {

    @Input
    var instances: List<Instance> = aem.instances

    @get:Internal
    val instanceHandles: List<LocalHandle>
        get() = instances.toLocalHandles(aem.project)

    @Internal
    var notify = true

    fun notify(title: String, text: String, enabled: Boolean = this.notify) {
        if (enabled) {
            aem.notifier.notify(title, text)
        } else {
            aem.notifier.log(title, text)
        }
    }
}