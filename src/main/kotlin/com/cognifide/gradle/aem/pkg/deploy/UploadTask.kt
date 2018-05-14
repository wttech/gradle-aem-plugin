package com.cognifide.gradle.aem.pkg.deploy

import com.cognifide.gradle.aem.api.AemTask
import com.cognifide.gradle.aem.instance.Instance
import org.gradle.api.tasks.TaskAction

open class UploadTask : SyncTask() {

    companion object {
        val NAME = "aemUpload"
    }

    init {
        group = AemTask.GROUP
        description = "Uploads CRX package to instance(s)."
    }

    @TaskAction
    fun upload() {
        val pkg = config.packageFile
        val instances = Instance.filter(project)

        synchronizeInstances(instances, { it.uploadPackage(pkg) })

        notifier.default("Package uploaded", "${pkg.name} on ${instances.joinToString(", ") { it.name }}")
    }

}
