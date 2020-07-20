package com.cognifide.gradle.sling.pkg.tasks

import com.cognifide.gradle.sling.common.instance.names
import com.cognifide.gradle.sling.common.tasks.PackageTask
import com.cognifide.gradle.sling.common.utils.fileNames
import org.gradle.api.tasks.TaskAction

open class PackagePurge : PackageTask() {

    @TaskAction
    fun purge() {
        sync { awaitIf { packageManager.purge(it) } }
        common.notifier.notify("Package purged", "${files.files.fileNames} from ${instances.get().names}")
    }

    init {
        description = "Uninstalls and then deletes CRX package on Sling instance(s)."
        sling.prop.boolean("package.purge.awaited")?.let { awaited.set(it) }
        checkForce()
    }

    companion object {
        const val NAME = "packagePurge"
    }
}
