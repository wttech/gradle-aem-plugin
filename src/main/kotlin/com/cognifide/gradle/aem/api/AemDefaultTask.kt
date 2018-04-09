package com.cognifide.gradle.aem.api

import com.cognifide.gradle.aem.internal.PropertyParser
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested

abstract class AemDefaultTask : DefaultTask(), AemTask {

    @Nested
    final override val config = AemConfig.of(project)

    @Internal
    protected val propertyParser = PropertyParser(project)

    init {
        group = AemTask.GROUP
    }

}