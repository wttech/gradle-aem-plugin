package com.cognifide.gradle.aem.pkg.tasks

import com.cognifide.gradle.aem.common.instance.checkAvailable
import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.PackageTask
import com.cognifide.gradle.aem.common.utils.fileNames
import org.gradle.api.tasks.TaskAction

open class PackagePurge : PackageTask() {

    @TaskAction
    fun purge() {
        instances.checkAvailable()
        sync { packageManager.purge(it) }
        common.notifier.notify("Package purged", "${packages.fileNames} from ${instances.names}")
    }

    init {
        description = "Uninstalls and then deletes CRX package on AEM instance(s)."
        awaited = aem.prop.boolean("package.purge.awaited") ?: true
        checkForce()
    }

    companion object {
        const val NAME = "packagePurge"
    }
}
