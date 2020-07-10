package com.cognifide.gradle.sling.pkg.tasks

import com.cognifide.gradle.sling.common.instance.names
import com.cognifide.gradle.sling.common.tasks.PackageTask
import com.cognifide.gradle.sling.common.utils.fileNames
import org.gradle.api.tasks.TaskAction

open class PackageUpload : PackageTask() {

    @TaskAction
    fun upload() {
        sync { packageManager.upload(it) }
        common.notifier.notify("Package uploaded", "${files.files.fileNames} from ${instances.get().names}")
    }

    init {
        description = "Uploads Sling package to instance(s)."
        awaited.convention(false)
    }

    companion object {
        const val NAME = "packageUpload"
    }
}
