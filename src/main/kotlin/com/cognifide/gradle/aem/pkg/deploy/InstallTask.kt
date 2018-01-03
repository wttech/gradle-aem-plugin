package com.cognifide.gradle.aem.pkg.deploy

import com.cognifide.gradle.aem.base.api.AemTask
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
