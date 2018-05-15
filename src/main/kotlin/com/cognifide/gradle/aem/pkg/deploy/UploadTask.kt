package com.cognifide.gradle.aem.pkg.deploy

import com.cognifide.gradle.aem.api.AemDefaultTask
import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.instance.names
import com.cognifide.gradle.aem.instance.sync
import org.gradle.api.tasks.TaskAction

open class UploadTask : AemDefaultTask() {

    companion object {
        val NAME = "aemUpload"
    }

    init {
        description = "Uploads CRX package to instance(s)."
    }

    @TaskAction
    fun upload() {
        val pkg = config.packageFile
        val instances = Instance.filter(project)

        instances.sync(project, { it.uploadPackage(pkg) })

        notifier.default("Package uploaded", "${pkg.name} on ${instances.names}")
    }

}
