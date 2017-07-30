package com.cognifide.gradle.aem

import com.cognifide.gradle.aem.deploy.CreateTask
import com.cognifide.gradle.aem.deploy.DestroyTask
import com.cognifide.gradle.aem.deploy.DownTask
import com.cognifide.gradle.aem.deploy.UpTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.language.base.plugins.LifecycleBasePlugin

/**
 * Separate plugin which provides tasks for managing local instances.
 * Can be applied only to one project in build.
 */
class AemInstancePlugin : Plugin<Project> {

    companion object {
        val ID = "com.cognifide.aem.instance"

        val FILES_PATH = "local-instance"
    }

    override fun apply(project: Project) {
        preventAppliedMultiple(project)
        setupDependentPlugins(project)
        setupTasks(project)
    }

    private fun preventAppliedMultiple(project: Project) {
        project.allprojects.forEach { subproject ->
            if (subproject.plugins.hasPlugin(ID)) {
                throw AemException("AEM instance plugin cannot be applied to more than one project.")
            }
        }
    }

    private fun setupDependentPlugins(project: Project) {
        project.plugins.apply(AemBasePlugin::class.java)
    }

    private fun setupTasks(project: Project) {
        val clean = project.tasks.getByName(LifecycleBasePlugin.CLEAN_TASK_NAME)

        val create = project.tasks.create(CreateTask.NAME, CreateTask::class.java)
        val destroy = project.tasks.create(DestroyTask.NAME, DestroyTask::class.java)
        val up = project.tasks.create(UpTask.NAME, UpTask::class.java)
        val down = project.tasks.create(DownTask.NAME, DownTask::class.java)

        create.mustRunAfter(clean)
        up.mustRunAfter(clean)
        up.dependsOn(create)
        destroy.mustRunAfter(down)
    }

}