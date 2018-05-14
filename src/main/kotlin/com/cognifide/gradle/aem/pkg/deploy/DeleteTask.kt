package com.cognifide.gradle.aem.pkg.deploy

import com.cognifide.gradle.aem.api.AemTask
import com.cognifide.gradle.aem.instance.Instance
import org.gradle.api.tasks.TaskAction

open class DeleteTask : SyncTask() {

    companion object {
        val NAME = "aemDelete"
    }

    init {
        group = AemTask.GROUP
        description = "Deletes AEM package on instance(s)."
    }

    @TaskAction
    fun delete() {
        propertyParser.checkForce()

        val instances = Instance.filter(project)
        val pkg = config.packageFileName

        notifier.default("Package deleted", "$pkg on ${instances.joinToString(", ") { it.name }}")
    }

}
