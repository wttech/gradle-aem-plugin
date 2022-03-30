package com.cognifide.gradle.aem.launcher

import java.io.File
import java.util.*

class BuildScaffolder(private val launcher: Launcher) {

    fun scaffold() {
        saveProperties()
        saveSettings()
        saveRootBuildScript()

        if (propertyAemVersion()?.isNotBlank() == true) {
            if (propertyAemVersion() == "cloud") EnvCloudScaffolder(launcher).scaffold() else EnvOnPremScaffolder(launcher).scaffold()
        } else {
            EnvScaffolder(launcher).scaffold()
        }
    }

    fun propertyAemVersion() = archetypeProperties()?.getProperty("aemVersion")

    fun archetypeProperties() = if (File("archetype.properties").exists()) Properties().apply {
        File("archetype.properties").inputStream().buffered().use { load(it) }
    } else null

    private fun saveProperties() = launcher.workFileOnce("gradle.properties") {
        println("Saving Gradle properties file '$this'")
        outputStream().use { output ->
            Properties().apply {
                putAll(defaultProps)
                if (savePropsFlag) {
                    putAll(saveProps)
                }
                store(output, null)
            }
        }
    }

    private val savePropsFlag get() = launcher.args.contains(Launcher.ARG_SAVE_PROPS)

    private val saveProps get() = launcher.args.filter { it.startsWith(Launcher.ARG_SAVE_PREFIX) }
        .map { it.removePrefix(Launcher.ARG_SAVE_PREFIX) }
        .map { it.substringBefore("=") to it.substringAfter("=") }
        .toMap()

    private val defaultProps get() = mapOf(
        "org.gradle.logging.level" to "info",
        "org.gradle.daemon" to "true",
        "org.gradle.parallel" to "true",
        "org.gradle.caching" to "true",
        "org.gradle.jvmargs" to "-Xmx2048m -XX:MaxPermSize=512m -Dfile.encoding=UTF-8"
    )

    private fun saveRootBuildScript() = launcher.workFileOnce("build.gradle.kts") {
        println("Saving root Gradle build script file '$this'")
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
