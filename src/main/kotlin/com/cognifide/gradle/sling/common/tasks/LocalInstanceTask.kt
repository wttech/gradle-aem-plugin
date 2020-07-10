package com.cognifide.gradle.sling.common.tasks

import com.cognifide.gradle.sling.SlingDefaultTask
import com.cognifide.gradle.sling.common.instance.LocalInstance
import org.gradle.api.tasks.Internal

open class LocalInstanceTask : SlingDefaultTask() {

    @Internal
    val instances = sling.obj.list<LocalInstance> {
        convention(sling.obj.provider { sling.localInstances })
    }

    @get:Internal
    val localInstanceManager get() = sling.localInstanceManager
}
