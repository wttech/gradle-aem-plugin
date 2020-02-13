package com.cognifide.gradle.aem

import com.cognifide.gradle.common.CommonDefaultTask
import org.gradle.api.tasks.Internal

open class AemDefaultTask : CommonDefaultTask(), AemTask {

    @Internal
    final override val aem = project.aem

    init {
        group = AemTask.GROUP
    }
}
