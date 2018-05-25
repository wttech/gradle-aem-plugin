package com.cognifide.gradle.aem.pkg.deploy

import com.cognifide.gradle.aem.api.AemDefaultTask
import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.instance.names
import com.cognifide.gradle.aem.instance.sync
import org.gradle.api.tasks.TaskAction

open class DeleteTask : AemDefaultTask() {

    companion object {
        val NAME = "aemDelete"
    }

    init {
        description = "Deletes AEM package on instance(s)."
    }

    @TaskAction
    fun delete() {
        props.checkForce()

        val instances = Instance.filter(project)
        val pkg = config.packageFileName

        instances.sync(project, { it.deletePackage(it.determineRemotePackagePath()) })

        notifier.default("Package deleted", "$pkg on ${instances.names}")
    }

}
