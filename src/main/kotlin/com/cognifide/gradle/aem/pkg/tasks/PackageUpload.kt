package com.cognifide.gradle.aem.pkg.tasks

import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.Package
import com.cognifide.gradle.aem.common.utils.fileNames
import org.gradle.api.tasks.TaskAction

open class PackageUpload : Package() {

    @TaskAction
    fun upload() {
        sync.action { packageManager.upload(it) }
        common.notifier.notify("Package uploaded", "${files.fileNames} from ${instances.names}")
    }

    init {
        description = "Uploads AEM package to instance(s)."
        sync.awaited.convention(false)
    }

    companion object {
        const val NAME = "packageUpload"
    }
}
