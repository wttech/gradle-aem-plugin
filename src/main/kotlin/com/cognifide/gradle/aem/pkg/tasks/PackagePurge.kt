package com.cognifide.gradle.aem.pkg.tasks

import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.Package
import com.cognifide.gradle.aem.common.utils.fileNames
import org.gradle.api.tasks.TaskAction

open class PackagePurge : Package() {

    @TaskAction
    fun purge() {
        sync.actionAwaited { packageManager.purge(it) }
        common.notifier.notify("Package purged", "${files.fileNames} from ${instances.names}")
    }

    init {
        description = "Uninstalls and then deletes CRX package on AEM instance(s)."
        aem.prop.boolean("package.purge.awaited")?.let { sync.awaited.set(it) }
        checkForce()
    }

    companion object {
        const val NAME = "packagePurge"
    }
}
