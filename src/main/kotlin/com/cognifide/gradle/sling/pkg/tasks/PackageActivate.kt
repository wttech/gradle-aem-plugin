package com.cognifide.gradle.sling.pkg.tasks

import com.cognifide.gradle.sling.common.instance.names
import com.cognifide.gradle.sling.common.tasks.PackageTask
import com.cognifide.gradle.sling.common.utils.fileNames
import org.gradle.api.tasks.TaskAction

open class PackageActivate : PackageTask() {

    @TaskAction
    fun activate() {
        sync { packageManager.activate(it) }
        common.notifier.notify("Package activated", "${files.files.fileNames} on ${instances.get().names}")
    }

    init {
        description = "Activates CRX package on instance(s)."
        sling.prop.boolean("package.activate.awaited")?.let { awaited.set(it) }
    }

    companion object {
        const val NAME = "packageActivate"
    }
}
