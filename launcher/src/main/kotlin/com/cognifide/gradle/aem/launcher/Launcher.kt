package com.cognifide.gradle.aem.launcher

import org.gradle.tooling.GradleConnector
import java.io.File
import java.util.*
import kotlin.system.exitProcess

class Launcher(private val args: Array<String>) {

    val saveProps get() = args.contains(ARG_SAVE_PROPS)

    val printStackTrace get() = args.contains(ARG_PRINT_STACKTRACE)

    val colorOutput get() = !args.contains(ARG_NO_COLOR_OUTPUT)

    val workDirPath get() = args.firstOrNull { it.startsWith("$ARG_WORK_DIR=") }?.substringAfter("=")

    val gradleArgs get() = args.filterNot { ARGS.contains(it) || ARGS.any { arg -> it.startsWith("$arg=") } }

    val buildConfig get() = Properties().apply {
        load(Launcher::class.java.getResourceAsStream("/build.properties"))
    }
    val gradleVersion get() = buildConfig["gradleVersion"]?.toString()
        ?: throw LauncherException("Gradle version info not available!")

    val pluginVersion get() = buildConfig["pluginVersion"]?.toString()
        ?: throw LauncherException("AEM Plugin version info not available!")

    val currentDir get() = File(".")

    val workDir get() = if (workDirPath != null) currentDir.resolve(workDirPath!!) else currentDir

    fun workFile(path: String, action: File.() -> Unit) {
        workDir.resolve(path).apply {
            if (exists()) return@apply
            parentFile.mkdirs()
            action()
        }
    }

    fun saveBuildSrc() {
        workFile("buildSrc/build.gradle.kts") {
            writeText("""
                repositories {
                    mavenLocal()
                    jcenter()
                    gradlePluginPortal()
                }
                
                dependencies {
                    implementation("com.cognifide.gradle:aem-plugin:$pluginVersion")
                    implementation("com.cognifide.gradle:environment-plugin:1.1.27")
                    implementation("com.neva.gradle:fork-plugin:6.0.5")
                }
            """.trimIndent())
        }
    }

    fun saveProperties() {
        if (saveProps) {
            workFile("gradle.properties") {
                outputStream().use { output ->
                    Properties().apply {
                        val props = args.filter { it.startsWith(ARG_SAVE_PREFIX) }
                            .map { it.removePrefix(ARG_SAVE_PREFIX) }
                            .map { it.substringBefore("=") to it.substringAfter("=") }
                            .toMap()
                        putAll(props)
                        store(output, null)
                    }
                }
            }
        }
    }

    fun saveRootBuildScript() = workFile("build.gradle.kts") {
        writeText("""
            plugins {
                id("com.cognifide.aem.common")
            }
            
            aem {
                mvnBuild {
                    discover()
                }
            }
        """.trimIndent())
    }

    fun saveEnvBuildScript() = workFile("env/build.gradle.kts") {
        writeText("""
            plugins {
                id("com.cognifide.aem.instance.local")
            }
        """.trimIndent())
    }

    fun saveSettings() = workFile("settings.gradle.kts") {
        writeText("""
            include(":env")
            
        """.trimIndent())
    }

    fun runBuild(): Unit = try {
        GradleConnector.newConnector()
            .useGradleVersion(gradleVersion)
            .useBuildDistribution()
            .forProjectDirectory(workDir)
            .connect().use { connection ->
                connection.newBuild()
                    .withArguments(gradleArgs)
                    .setColorOutput(colorOutput)
                    .setStandardOutput(System.out)
                    .setStandardError(System.err)
                    .setStandardInput(System.`in`)
                    .run()
            }
        exitProcess(0)
    } catch (e: Exception) {
        if (printStackTrace) {
            e.printStackTrace(System.err)
        }
        exitProcess(1)
    }

    companion object {

        val ARG_WORK_DIR = "--work-dir"

        val ARG_PRINT_STACKTRACE = "--print-stacktrace"

        val ARG_NO_COLOR_OUTPUT = "--no-color"

        val ARG_SAVE_PROPS = "--save-props"

        val ARG_SAVE_PREFIX = "-P"

        val ARGS = listOf(ARG_SAVE_PROPS, ARG_PRINT_STACKTRACE, ARG_NO_COLOR_OUTPUT, ARG_WORK_DIR)

        @JvmStatic
        fun main(args: Array<String>) {
            Launcher(args).apply {
                saveBuildSrc()
                saveProperties()
                saveSettings()
                saveRootBuildScript()
                saveEnvBuildScript()

                runBuild()
            }
        }
    }
}

