package com.cognifide.gradle.aem.common.tasks

import com.cognifide.gradle.aem.pkg.tasks.PackageCompose

open class PackageTask : SyncFileTask() {

    init {
        files.from(common.tasks.getAll<PackageCompose>().map { it.composedFile }) // TODO realizablecollection / do flatMap as for providers
    }
}
