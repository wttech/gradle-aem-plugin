package com.cognifide.gradle.aem.common.tasks

import com.cognifide.gradle.aem.AemDefaultTask
import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.InstanceException
import org.gradle.api.tasks.Internal

open class Instance : AemDefaultTask() {

    @Internal
    val instances = aem.obj.list<Instance> {
        convention(aem.obj.provider { aem.instances })
    }

    @get:Internal
    val anyInstances: List<Instance> by lazy {
        instances.get().apply {
            if (aem.commonOptions.verbose.get() && isEmpty()) {
                throw InstanceException("No instances defined or matching filter '${aem.commonOptions.envFilter}'!")
            }
        }
    }

    @get:Internal
    val instanceManager get() = aem.instanceManager
}
