package com.cognifide.gradle.aem

import com.cognifide.gradle.aem.deploy.*
import com.cognifide.gradle.aem.jar.UpdateManifestTask
import com.cognifide.gradle.aem.pkg.ComposeTask
import com.cognifide.gradle.aem.vlt.CheckoutTask
import com.cognifide.gradle.aem.vlt.CleanTask
import com.cognifide.gradle.aem.vlt.SyncTask
import com.cognifide.gradle.aem.vlt.VltTask
import com.google.common.base.CaseFormat
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.language.base.plugins.LifecycleBasePlugin

/**
 * Plugin assumptions:
 *
 * JVM based languages like Groovy or Kotlin have implicitly applied 'java' plugin.
 * Projects can have only 'com.cognifide.aem' plugin applied intentionally to generate packages with content only.
 * Projects can have applied official 'osgi' or 'org.dm.bundle' plugins to customize OSGi manifest.
 */
class AemPlugin : Plugin<Project> {

    companion object {
        val ID = "com.cognifide.aem"

        val TASK_GROUP = "AEM"

        val CONFIG_INSTALL = "aemInstall"

        val CONFIG_EMBED = "aemEmbed"

        val VLT_PATH = "META-INF/vault"

        val JCR_ROOT = "jcr_root"

        val DEPLOY_TASK_ROOT = "aemRootDeploy"

        val DEPLOY_TASK_RULE = "Pattern: aem<ProjectPath>Deploy: Build CRX package and deploy it to AEM instance(s). Use preemptive '$DEPLOY_TASK_ROOT' for root project."
    }

    override fun apply(project: Project) {
        setupDependentPlugins(project)
        setupExtensions(project)
        setupTasks(project)
        setupConfigurations(project)
        setupConfig(project)
    }

    private fun setupDependentPlugins(project: Project) {
        project.plugins.apply(BasePlugin::class.java)
    }

    private fun setupExtensions(project: Project) {
        project.extensions.create(AemExtension.NAME, AemExtension::class.java)
    }

    private fun setupTasks(project: Project) {
        val clean = project.tasks.getByName(LifecycleBasePlugin.CLEAN_TASK_NAME)

        project.plugins.withType(JavaPlugin::class.java, {
            val classes = project.tasks.getByName(JavaPlugin.CLASSES_TASK_NAME)
            val testClasses = project.tasks.getByName(JavaPlugin.TEST_CLASSES_TASK_NAME)
            val updateManifest = project.tasks.create(UpdateManifestTask.NAME, UpdateManifestTask::class.java)
            val jar = project.tasks.getByName(JavaPlugin.JAR_TASK_NAME)

            updateManifest.dependsOn(classes, testClasses)
            jar.dependsOn(updateManifest)
        })

        val compose = project.tasks.create(ComposeTask.NAME, ComposeTask::class.java)
        val upload = project.tasks.create(UploadTask.NAME, UploadTask::class.java)
        val install = project.tasks.create(InstallTask.NAME, InstallTask::class.java)
        val activate = project.tasks.create(ActivateTask.NAME, ActivateTask::class.java)
        val deploy = project.tasks.create(DeployTask.NAME, DeployTask::class.java)
        val distribute = project.tasks.create(DistributeTask.NAME, DistributeTask::class.java)
        val satisfy = project.tasks.create(SatisfyTask.NAME, SatisfyTask::class.java)

        val assemble = project.tasks.getByName(LifecycleBasePlugin.ASSEMBLE_TASK_NAME)
        val check = project.tasks.getByName(LifecycleBasePlugin.CHECK_TASK_NAME)
        val build = project.tasks.getByName(LifecycleBasePlugin.BUILD_TASK_NAME)

        assemble.mustRunAfter(clean)
        check.mustRunAfter(clean)
        build.dependsOn(compose)

        compose.dependsOn(assemble, check)
        compose.mustRunAfter(clean)

        upload.mustRunAfter(satisfy, compose)
        install.mustRunAfter(satisfy, compose, upload)
        activate.mustRunAfter(satisfy, compose, upload, install)

        deploy.mustRunAfter(satisfy, compose)
        distribute.mustRunAfter(satisfy, compose)

        satisfy.mustRunAfter(clean)

        val vltClean = project.tasks.create(CleanTask.NAME, CleanTask::class.java)
        val vltRaw = project.tasks.create(VltTask.NAME, VltTask::class.java)
        val vltCheckout = project.tasks.create(CheckoutTask.NAME, CheckoutTask::class.java)
        val vltSync = project.tasks.create(SyncTask.NAME, SyncTask::class.java)

        vltClean.mustRunAfter(clean)
        vltRaw.mustRunAfter(clean)
        vltCheckout.mustRunAfter(clean)
        vltSync.mustRunAfter(clean)

        project.tasks.addRule(DEPLOY_TASK_RULE, { taskName ->
            val desiredTaskName = if (project == project.rootProject) {
                DEPLOY_TASK_ROOT
            } else {
                "aem${CaseFormat.LOWER_HYPHEN.to(CaseFormat.UPPER_CAMEL, project.path.replace(":", "-"))}Deploy"
            }

            if (taskName == desiredTaskName) {
                if (project.tasks.findByName(taskName) != null) {
                    project.logger.info("Deploy rule task '$taskName' already exists, so it will be not created.")
                } else {
                    project.logger.info("Creating deploy rule task named '$taskName'.")

                    val task = project.tasks.create(taskName)
                    task.dependsOn(build, deploy)
                }
            }
        })
    }

    private fun setupConfigurations(project: Project) {
        project.plugins.withType(JavaPlugin::class.java, {
            val baseConfig = project.configurations.getByName(JavaPlugin.COMPILE_CONFIGURATION_NAME)
            val configurer: (Configuration) -> Unit = {
                it.isTransitive = false
                baseConfig.extendsFrom(it)
            }

            project.configurations.create(CONFIG_EMBED, configurer)
            project.configurations.create(CONFIG_INSTALL, configurer)
        })
    }

    private fun setupConfig(project: Project) {
        project.tasks.forEach { task ->
            if (task is AemTask && task is DefaultTask) {
                task.config.configure(task)
            }
        }

        project.afterEvaluate({
            project.tasks.forEach { task ->
                if (task is AemTask && task is DefaultTask) {
                    task.config.validate()
                    task.config.attach(task)
                }
            }
        })
    }

}