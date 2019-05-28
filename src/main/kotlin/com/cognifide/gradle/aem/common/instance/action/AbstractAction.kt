package com.cognifide.gradle.aem.common.instance.action

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.InstanceAction
import com.cognifide.gradle.aem.common.instance.LocalInstance
import org.gradle.api.tasks.Internal

abstract class AbstractAction(
    @Internal
    @Transient
    val aem: AemExtension
) : InstanceAction {

    var enabled = true

    var instances: List<Instance> = aem.instances

    val localInstances: List<LocalInstance>
        get() = instances.filterIsInstance(LocalInstance::class.java)

    var notify = true

    fun notify(title: String, text: String, enabled: Boolean = this.notify) {
        if (enabled) {
            aem.notifier.notify(title, text)
        } else {
            aem.notifier.log(title, text)
        }
    }
}