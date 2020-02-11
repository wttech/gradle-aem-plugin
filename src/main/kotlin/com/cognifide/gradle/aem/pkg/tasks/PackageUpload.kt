package com.cognifide.gradle.aem.pkg.tasks

import com.cognifide.gradle.aem.common.instance.checkAvailable
import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.PackageTask
import com.cognifide.gradle.aem.common.utils.fileNames
import org.gradle.api.tasks.TaskAction

open class PackageUpload : PackageTask() {

    @TaskAction
    fun upload() {
        instances.get().checkAvailable()
        sync { packageManager.upload(it) }
        common.notifier.notify("Package uploaded", "${packages.get().fileNames} from ${instances.get().names}")
    }

    init {
        description = "Uploads AEM package to instance(s)."
        awaited.convention(false)
    }

    companion object {
        const val NAME = "packageUpload"
    }
}
