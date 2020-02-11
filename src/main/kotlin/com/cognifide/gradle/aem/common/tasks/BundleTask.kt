package com.cognifide.gradle.aem.common.tasks

import org.gradle.api.tasks.Internal

open class BundleTask : SyncFileTask() {

    @get:Internal
    val bundles get() = files

    init {
        bundles.convention(aem.obj.provider { aem.dependentBundles(this) })
    }
}
