package com.cognifide.gradle.aem.pkg.tasks

import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.Package
import com.cognifide.gradle.aem.common.utils.fileNames
import org.gradle.api.tasks.TaskAction

open class PackageActivate : Package() {

    @TaskAction
    override fun doSync() {
        super.doSync()
        common.notifier.notify("Package activated", "${files.fileNames} on ${instances.names}")
    }

    init {
        description = "Activates CRX package on instance(s)."
        sync.action { packageManager.activate(it) }
        aem.prop.boolean("package.activate.awaited")?.let { sync.awaited.set(it) }
    }

    companion object {
        const val NAME = "packageActivate"
    }
}
