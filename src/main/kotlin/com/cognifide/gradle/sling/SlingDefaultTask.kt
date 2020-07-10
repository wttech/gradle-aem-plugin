package com.cognifide.gradle.sling

import com.cognifide.gradle.common.CommonDefaultTask
import org.gradle.api.tasks.Internal

open class SlingDefaultTask : CommonDefaultTask(), SlingTask {

    @Internal
    final override val sling = project.sling

    init {
        group = SlingTask.GROUP
    }
}
