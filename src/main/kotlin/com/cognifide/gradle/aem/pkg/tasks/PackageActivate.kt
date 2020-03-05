package com.cognifide.gradle.aem.pkg.tasks

import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.PackageTask
import com.cognifide.gradle.aem.common.utils.fileNames
import org.gradle.api.tasks.TaskAction

open class PackageActivate : PackageTask() {

    @TaskAction
    fun activate() {
        sync { packageManager.activate(it) }
        common.notifier.notify("Package activated", "${files.files.fileNames} on ${instances.get().names}")
    }

    init {
        description = "Activates CRX package on instance(s)."
        aem.prop.boolean("package.activate.awaited")?.let { awaited.set(it) }
    }

    companion object {
        const val NAME = "packageActivate"
    }
}
