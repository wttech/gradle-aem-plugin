package com.cognifide.gradle.aem.common.tasks

import com.cognifide.gradle.aem.AemDefaultTask
import com.cognifide.gradle.aem.common.instance.LocalInstance
import com.cognifide.gradle.aem.common.instance.LocalInstanceException
import org.gradle.api.tasks.Internal

open class LocalInstance : AemDefaultTask() {

    @Internal
    val instances = aem.obj.list<LocalInstance> {
        convention(aem.obj.provider { aem.localInstances })
    }

    val anyInstances: List<LocalInstance> get() = instances.get().apply {
        if (aem.commonOptions.verbose.get() && isEmpty()) {
            throw LocalInstanceException("No local instances defined!\nMost probably there are no instances matching filter '${aem.commonOptions.envFilter}'.")
        }
    }

    @get:Internal
    val localInstanceManager get() = aem.localInstanceManager
}
