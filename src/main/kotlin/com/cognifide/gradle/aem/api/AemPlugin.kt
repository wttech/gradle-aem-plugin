package com.cognifide.gradle.aem.api

import com.fasterxml.jackson.databind.ObjectMapper
import java.io.Serializable
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider

abstract class AemPlugin : Plugin<Project> {

    fun <T : Task> Project.registerTask(name: String, clazz: Class<T>): TaskProvider<T> {
        return registerTask(name, clazz, Action {})
    }

    fun <T : Task> Project.registerTask(name: String, clazz: Class<T>, configurer: (T) -> Unit): TaskProvider<T> {
        return registerTask(name, clazz, Action { configurer(it) })
    }

    fun <T : Task> Project.registerTask(name: String, clazz: Class<T>, configurer: Action<T>): TaskProvider<T> {
        val provider = tasks.register(name, clazz, configurer)

        afterEvaluate { provider.configure { if (it is AemTask) it.projectEvaluated() } }
        gradle.projectsEvaluated { provider.configure { if (it is AemTask) it.projectsEvaluated() } }
        gradle.taskGraph.whenReady { graph -> provider.configure { if (it is AemTask) it.taskGraphReady(graph) } }

        return provider
    }

    override fun apply(project: Project) {
        with(project) { configure() }
    }

    abstract fun Project.configure()

    class Build : Serializable {

        lateinit var pluginVersion: String

        lateinit var gradleVersion: String
    }

    companion object {

        val BUILD by lazy {
            fromJson(AemPlugin::class.java.getResourceAsStream("/build.json")
                    .bufferedReader().use { it.readText() })
        }

        val ID = "gradle-aem-plugin"

        val NAME = "Gradle AEM Plugin"

        val NAME_WITH_VERSION: String
            get() = "$NAME ${BUILD.pluginVersion}"

        private fun fromJson(json: String): Build {
            return ObjectMapper().readValue(json, Build::class.java)
        }

        private var once = false

        @Synchronized
        fun once(callback: (Build) -> Unit) {
            if (!once) {
                callback(BUILD)
                once = true
            }
        }
    }
}