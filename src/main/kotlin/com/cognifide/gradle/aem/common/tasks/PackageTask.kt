package com.cognifide.gradle.aem.common.tasks

import org.gradle.api.tasks.Internal

open class PackageTask : SyncFileTask() {

    @get:Internal
    val packages get() = files

    init {
        packages.convention(aem.obj.provider { aem.dependentPackages(this) })
    }
}
