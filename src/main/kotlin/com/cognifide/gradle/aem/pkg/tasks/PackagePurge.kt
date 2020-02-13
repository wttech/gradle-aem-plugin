package com.cognifide.gradle.aem.pkg.tasks

import com.cognifide.gradle.aem.common.instance.checkAvailable
import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.PackageTask
import com.cognifide.gradle.aem.common.utils.fileNames
import org.gradle.api.tasks.TaskAction

open class PackagePurge : PackageTask() {

    @TaskAction
    fun purge() {
        instances.get().checkAvailable()
        sync { packageManager.purge(it) }
        common.notifier.notify("Package purged", "${files.files.fileNames} from ${instances.get().names}")
    }

    init {
        description = "Uninstalls and then deletes CRX package on AEM instance(s)."
        aem.prop.boolean("package.purge.awaited")?.let { awaited.set(it) }
        checkForce()
    }

    companion object {
        const val NAME = "packagePurge"
    }
}
