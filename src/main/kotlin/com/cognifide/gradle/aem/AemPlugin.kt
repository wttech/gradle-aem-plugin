package com.cognifide.gradle.aem

import com.fasterxml.jackson.databind.ObjectMapper
import java.io.Serializable
import org.gradle.api.Plugin
import org.gradle.api.Project

abstract class AemPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        with(project) { configure() }
    }

    abstract fun Project.configure()

    protected fun Project.tasks(configurer: AemTaskFacade.() -> Unit) {
        return AemExtension.of(this).tasks(configurer)
    }

    class Build : Serializable {

        lateinit var pluginVersion: String

        lateinit var gradleVersion: String
    }

    companion object {

        const val PKG = "com.cognifide.gradle.aem"

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

        fun withId(project: Project, id: String): List<Project> {
            return project.rootProject.allprojects.filter { it.plugins.hasPlugin(id) }
        }
    }
}
