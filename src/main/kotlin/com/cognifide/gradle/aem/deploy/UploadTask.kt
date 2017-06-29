package com.cognifide.gradle.aem.deploy

import com.cognifide.gradle.aem.AemPlugin
import org.gradle.api.tasks.TaskAction

open class UploadTask : AbstractTask() {

    companion object {
        val NAME = "aemUpload"
    }

    init {
        group = AemPlugin.TASK_GROUP
        description = "Uploads CRX package to instance(s)."
    }

    @TaskAction
    fun upload() {
        deploy({ sync ->
            uploadPackage(determineLocalPackage(), sync)
        })
    }

}
