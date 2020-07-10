package com.cognifide.gradle.sling

import org.gradle.api.Task
import org.gradle.api.tasks.Internal

interface SlingTask : Task {

    @get:Internal
    val sling: SlingExtension

    companion object {
        const val GROUP = "Sling"
    }
}
