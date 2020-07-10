package com.cognifide.gradle.sling.pkg.tasks

import com.cognifide.gradle.sling.common.instance.names
import com.cognifide.gradle.sling.common.tasks.PackageTask
import com.cognifide.gradle.sling.common.utils.fileNames
import org.gradle.api.tasks.TaskAction

open class PackageDelete : PackageTask() {

    @TaskAction
    fun delete() {
        sync { packageManager.delete(it) }
        common.notifier.notify("Package deleted", "${files.files.fileNames} on ${instances.get().names}")
    }

    init {
        description = "Deletes Sling package on instance(s)."
        awaited.convention(false)
        checkForce()
    }

    companion object {
        const val NAME = "packageDelete"
    }
}
