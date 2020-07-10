package com.cognifide.gradle.sling.common.tasks

import com.cognifide.gradle.sling.SlingDefaultTask
import com.cognifide.gradle.sling.common.instance.Instance
import org.gradle.api.tasks.Internal

open class InstanceTask : SlingDefaultTask() {

    @Internal
    val instances = sling.obj.list<Instance> {
        convention(sling.obj.provider { sling.instances })
    }

    @get:Internal
    val instanceManager get() = sling.instanceManager
}
