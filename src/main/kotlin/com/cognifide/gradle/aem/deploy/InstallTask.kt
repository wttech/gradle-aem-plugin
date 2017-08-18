package com.cognifide.gradle.aem.deploy

import com.cognifide.gradle.aem.AemTask
import org.gradle.api.tasks.TaskAction

open class InstallTask : SyncTask() {

    companion object {
        val NAME = "aemInstall"
    }

    init {
        group = AemTask.GROUP
        description = "Installs CRX package on instance(s)."
    }

    @TaskAction
    fun install() {
        synchronizeInstances({ it.installPackage() })
    }

}
