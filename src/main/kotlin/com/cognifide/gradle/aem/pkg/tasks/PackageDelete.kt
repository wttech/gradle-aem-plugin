package com.cognifide.gradle.aem.pkg.tasks

import com.cognifide.gradle.aem.common.instance.checkAvailable
import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.PackageTask
import com.cognifide.gradle.aem.common.utils.fileNames
import org.gradle.api.tasks.TaskAction

open class PackageDelete : PackageTask() {

    @TaskAction
    fun delete() {
        instances.get().checkAvailable()
        sync { packageManager.delete(it) }
        common.notifier.notify("Package deleted", "${files.files.fileNames} on ${instances.get().names}")
    }

    init {
        description = "Deletes AEM package on instance(s)."
        awaited.convention(false)
        checkForce()
    }

    companion object {
        const val NAME = "packageDelete"
    }
}
