package com.cognifide.gradle.aem.base.api

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Nested

abstract class AemDefaultTask : DefaultTask(), AemTask {

    @Nested
    final override val config = AemConfig.of(project)

    init {
        group = AemTask.GROUP
    }

}