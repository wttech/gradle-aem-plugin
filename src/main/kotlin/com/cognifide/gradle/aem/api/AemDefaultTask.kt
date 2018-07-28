package com.cognifide.gradle.aem.api

import com.cognifide.gradle.aem.internal.PropertyParser
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested

abstract class AemDefaultTask : DefaultTask(), AemTask {

    @Nested
    final override val config = AemConfig.of(project)

    @Internal
    protected val notifier = AemNotifier.of(project)

    @Internal
    protected val props = PropertyParser(project)

    init {
        group = AemTask.GROUP
    }

    fun beforeExecuted(callback: AemDefaultTask.() -> Unit) {
        project.gradle.taskGraph.whenReady {
            if (it.hasTask(this@AemDefaultTask)) {
                callback()
            }
        }
    }

}