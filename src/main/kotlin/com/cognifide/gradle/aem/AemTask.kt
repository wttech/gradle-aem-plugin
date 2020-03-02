package com.cognifide.gradle.aem

import org.gradle.api.Task
import org.gradle.api.tasks.Internal

interface AemTask : Task {

    @get:Internal
    val aem: AemExtension

    companion object {
        const val GROUP = "AEM"
    }
}
