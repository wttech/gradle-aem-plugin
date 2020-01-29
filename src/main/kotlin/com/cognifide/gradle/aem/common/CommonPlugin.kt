package com.cognifide.gradle.aem.common

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.AemPlugin
import com.cognifide.gradle.aem.common.tasks.Debug
import com.cognifide.gradle.common.CommonDefaultPlugin
import org.gradle.api.GradleException
import java.util.*
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.gradle.language.base.plugins.LifecycleBasePlugin

/**
 * Provides 'aem' extension to build script on which all other build logic is based.
 */
class CommonPlugin : CommonDefaultPlugin() {

    override fun Project.configureProject() {
        setupGreet()
        setupDependentPlugins()
        setupStructureProperties()
        setupExtensions()
        setupTasks()
    }

    private fun Project.setupGreet() = AemPlugin.apply {
        once { logger.info("Using: $NAME_WITH_VERSION") }
    }

    private fun Project.setupDependentPlugins() {
        plugins.apply(BasePlugin::class.java)
    }

    private fun Project.setupStructureProperties() {
        val file = project.rootProject.file(STRUCTURE_PROPERTIES_FILE)
        if (file.exists()) {
            file.bufferedReader().use { r ->
                val props = Properties()
                props.load(r)
                props.forEach { k, v -> project.extensions.extraProperties.set(k as String, v) }
            }
        }
    }

    private fun Project.setupExtensions() {
        extensions.add(AemExtension.NAME, AemExtension(this))
    }

    private fun Project.setupTasks() = tasks {
        val options: Debug.() -> Unit = { mustRunAfter(LifecycleBasePlugin.BUILD_TASK_NAME) }
        try {
            register(Debug.NAME, options)
        } catch (e: GradleException) {
            register(Debug.NAME_FALLBACK, options)
        }
    }

    companion object {
        const val ID = "com.cognifide.aem.common"

        const val STRUCTURE_PROPERTIES_FILE = "aem.properties"
    }
}
