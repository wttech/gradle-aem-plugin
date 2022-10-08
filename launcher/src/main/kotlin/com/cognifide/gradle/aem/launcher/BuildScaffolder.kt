package com.cognifide.gradle.aem.launcher

import java.io.File
import java.util.Properties

class BuildScaffolder(private val launcher: Launcher) {

    fun scaffold() {
        saveProperties()
        saveSettings()
        saveRootBuildScript()
        when (aemVersion) {
            "cloud" -> EnvCloudScaffolder(launcher).scaffold()
            null -> EnvScaffolder(launcher).scaffold()
            else -> EnvOnPremScaffolder(launcher).scaffold()
        }
    }

    private val aemVersion get() = archetypeProperties?.getProperty("aemVersion")

    private val archetypeProperties get() = if (archetypePropertiesFile.exists()) Properties().apply {
        archetypePropertiesFile.inputStream().buffered().use { load(it) }
    } else null

    private val archetypePropertiesFile get() = File("archetype.properties")

    private fun saveProperties() = launcher.workFileOnce("gradle.properties") {
        println("Saving Gradle properties file '$this'")
        outputStream().use { output ->
            Properties().apply {
                putAll(defaultProps)
                if (savePropsFlag) {
                    putAll(saveProps)
                }
                putAll(projectProps)
                store(output, null)
            }
        }
    }

    private val savePropsFlag get() = launcher.args.contains(Launcher.ARG_SAVE_PROPS)

    private val saveProps get() = launcher.args.filter { it.startsWith(Launcher.ARG_SAVE_PREFIX) }
        .map { it.removePrefix(Launcher.ARG_SAVE_PREFIX) }
        .associate { it.substringBefore("=") to it.substringAfter("=") }

    private val defaultProps get() = mapOf(
        "org.gradle.logging.level" to "info",
        "org.gradle.daemon" to "true",
        "org.gradle.parallel" to "true",
        "org.gradle.caching" to "true",
        "org.gradle.jvmargs" to "-Xmx2048m -Dfile.encoding=UTF-8"
    )

    private val projectProps get() = mapOf("javaSupport.version" to "11")

    private fun saveRootBuildScript() = launcher.workFileOnce("build.gradle.kts") {
        println("Saving root Gradle build script file '$this'")

        val mavenRootDir = when {
            launcher.appDirPath.isNullOrBlank() -> ""
            else -> """rootDir("${launcher.appDirPath}")"""
        }

        writeText(
            """
            plugins {
                id("com.cognifide.aem.common")
                id("com.neva.fork")
            }
            
            apply(from = "gradle/fork/props.gradle.kts")
            if (file("gradle.user.properties").exists()) {
                if (aem.mvnBuild.available) defaultTasks(":env:setup")
                else defaultTasks(":env:instanceSetup")
            }
            
            allprojects {
                repositories {
                    mavenCentral()
                }
            }
            
            aem {
                mvnBuild {
                    $mavenRootDir
                    depGraph {
                        // softRedundantModule("ui.content" to "ui.apps")
                    }
                    discover()
                }
            }
            """.trimIndent()
        )
    }

    private fun saveSettings() = launcher.workFileOnce("settings.gradle.kts") {
        println("Saving Gradle settings file '$this'")
        writeText(
            """
            include(":env")
            """.trimIndent()
        )
    }
}
