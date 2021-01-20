package com.cognifide.gradle.aem.pkg.tasks

import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.Package
import com.cognifide.gradle.aem.common.utils.fileNames
import org.gradle.api.tasks.TaskAction

open class PackageDelete : Package() {

    @TaskAction
    override fun doSync() {
        super.doSync()
        common.notifier.notify("Package deleted", "${files.fileNames} on ${instances.names}")
    }

    init {
        description = "Deletes AEM package on instance(s)."
        sync.action { packageManager.delete(it) }
        sync.awaited.convention(false)
        checkForce()
    }

    companion object {
        const val NAME = "packageDelete"
    }
}
