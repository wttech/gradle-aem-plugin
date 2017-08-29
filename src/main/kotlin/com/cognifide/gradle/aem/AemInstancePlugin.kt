package com.cognifide.gradle.aem

import com.cognifide.gradle.aem.deploy.BuildTask
import com.cognifide.gradle.aem.instance.*
import com.cognifide.gradle.aem.pkg.ComposeTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.language.base.plugins.LifecycleBasePlugin

/**
 * Separate plugin which provides tasks for managing local instances.
 * Most often should be applied only to one project in build.
 * Applying it multiple times to same configuration could case confusing errors like AEM started multiple times.
 */
class AemInstancePlugin : Plugin<Project> {

    companion object {
        val ID = "com.cognifide.aem.instance"

        val FILES_PATH = "local-instance"
    }

    override fun apply(project: Project) {
        setupDependentPlugins(project)
        setupTasks(project)
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
        val satisfy = project.tasks.create(SatisfyTask.NAME, SatisfyTask::class.java)
        val await = project.tasks.create(AwaitTask.NAME, AwaitTask::class.java)
        val collect = project.tasks.create(CollectTask.NAME, CollectTask::class.java)

        create.mustRunAfter(clean)
        up.mustRunAfter(clean)
        up.dependsOn(create)
        destroy.mustRunAfter(down)
        satisfy.mustRunAfter(create, up)
        collect.mustRunAfter(satisfy)

        project.plugins.withId(AemPackagePlugin.ID, {
            val setup = project.tasks.create(SetupTask.NAME, SetupTask::class.java)
            val build = project.tasks.getByName(BuildTask.NAME)

            setup.dependsOn(create, up, satisfy, build, await)

            build.mustRunAfter(create, up, satisfy)
            await.mustRunAfter(build)
        })

        project.gradle.afterProject { subproject ->
            if (subproject.plugins.hasPlugin(AemPackagePlugin.ID)) {
                collect.dependsOn(subproject.tasks.getByName(ComposeTask.NAME))
            }
        }
    }

}