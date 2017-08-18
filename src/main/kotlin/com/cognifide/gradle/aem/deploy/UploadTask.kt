package com.cognifide.gradle.aem.deploy

import com.cognifide.gradle.aem.AemTask
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
        synchronizeInstances({ it.uploadPackage() })
    }

}
