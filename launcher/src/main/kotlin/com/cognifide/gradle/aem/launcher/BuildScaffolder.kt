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
                mavenCentral()
                gradlePluginPortal()
            }
            
            dependencies {
                implementation("com.cognifide.gradle:aem-plugin:${launcher.pluginVersion}")
                implementation("com.cognifide.gradle:common-plugin:1.0.41")
                implementation("com.neva.gradle:fork-plugin:7.0.5")
            }
        """.trimIndent())
    }

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
        writeText("""
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
        """.trimIndent())
    }

    private fun saveEnvBuildScript() = launcher.workFileOnce("env/build.gradle.kts") {
        println("Saving environment Gradle build script file '$this'")
        writeText("""
            plugins {
                id("com.cognifide.aem.instance.local")
            }
            
            val instancePassword = common.prop.string("instance.default.password")
            val publishHttpUrl = common.prop.string("publish.httpUrl") ?: aem.findInstance("local-publish")?.httpUrl ?: "http://127.0.0.1:4503"
            val servicePackUrl = common.prop.string("localInstance.spUrl")
            val coreComponentsUrl = common.prop.string("localInstance.coreComponentsUrl")

            aem {
                instance { // https://github.com/Cognifide/gradle-aem-plugin/blob/master/docs/instance-plugin.md
                    provisioner {
                        enableCrxDe()
                        servicePackUrl?.let { deployPackage(it) }
                        coreComponentsUrl?.let { deployPackage(it) }
                        configureReplicationAgentAuthor("publish") {
                            agent { configure(transportUri = "${'$'}publishHttpUrl/bin/receive?sling:authRequestLogin=1", transportUser = "admin", transportPassword = instancePassword, userId = "admin") }
                            version.set(publishHttpUrl)
                        }
                    }
                }
            }

            tasks {
                instanceSetup { if (rootProject.aem.mvnBuild.available) dependsOn(":all:deploy") }
                instanceResolve { dependsOn(":requireProps") }
                instanceCreate { dependsOn(":requireProps") }
            }
        """.trimIndent())
    }

    private fun saveSettings() = launcher.workFileOnce("settings.gradle.kts") {
        println("Saving Gradle settings file '$this'")
        writeText("""
            include(":env")
            
        """.trimIndent())
    }
}