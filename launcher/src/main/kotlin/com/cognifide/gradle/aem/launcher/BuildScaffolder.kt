package com.cognifide.gradle.aem.launcher

import java.util.*

class BuildScaffolder(private val launcher: Launcher) {

    fun scaffold() {
        saveBuildSrc()
        saveProperties()
        saveSettings()
        saveRootBuildScript()
        saveEnvBuildScript()
    }

    private fun saveBuildSrc() = launcher.workFileOnce("buildSrc/build.gradle.kts") {
        println("Saving Gradle build source script file '$this'")
        writeText("""
            repositories {
                mavenLocal()
                jcenter()
                gradlePluginPortal()
            }
            
            dependencies {
                implementation("com.cognifide.gradle:aem-plugin:${launcher.pluginVersion}")
                implementation("com.cognifide.gradle:environment-plugin:1.1.27")
                implementation("com.neva.gradle:fork-plugin:6.0.5")
            }
        """.trimIndent())
    }

    private fun saveProperties() {
        if (savePropsFlag) {
            launcher.workFileOnce("gradle.properties") {
                println("Saving Gradle properties to file '$this'")
                outputStream().use { output ->
                    Properties().apply {
                        putAll(saveProps)
                        store(output, null)
                    }
                }
            }
        }
    }

    private val savePropsFlag get() = launcher.args.contains(Launcher.ARG_SAVE_PROPS)

    private val saveProps get() = launcher.args.filter { it.startsWith(Launcher.ARG_SAVE_PREFIX) }
        .map { it.removePrefix(Launcher.ARG_SAVE_PREFIX) }
        .map { it.substringBefore("=") to it.substringAfter("=") }
        .toMap()

    private fun saveRootBuildScript() = launcher.workFileOnce("build.gradle.kts") {
        println("Saving root build script file '$this'")
        writeText("""
            plugins {
                id("com.cognifide.aem.common")
                id("com.neva.fork")
            }
            
            apply(from = "gradle/fork/props.gradle.kts")
            
            aem {
                mvnBuild {
                    discover()
                }
            }
        """.trimIndent())
    }

    private fun saveEnvBuildScript() = launcher.workFileOnce("env/build.gradle.kts") {
        println("Saving environment build script file '$this'")
        writeText("""
            plugins {
                id("com.cognifide.aem.instance.local")
            }
        """.trimIndent())
    }

    private fun saveSettings() = launcher.workFileOnce("settings.gradle.kts") {
        println("Saving settings file '$this'")
        writeText("""
            include(":env")
            
        """.trimIndent())
    }
}