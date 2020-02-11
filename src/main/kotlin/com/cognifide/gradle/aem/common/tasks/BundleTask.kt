package com.cognifide.gradle.aem.common.tasks

import com.cognifide.gradle.aem.bundle.tasks.BundleCompose

open class BundleTask : SyncFileTask() {

    init {
        files.from(common.tasks.getAll<BundleCompose>().map { it.composedFile }) // TODO realizablecollection / do flatMap as for providers
    }
}
