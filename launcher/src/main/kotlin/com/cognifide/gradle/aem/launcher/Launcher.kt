package com.cognifide.gradle.aem.launcher

import org.gradle.tooling.GradleConnector
import java.io.File
import java.util.*
import kotlin.system.exitProcess

class Launcher {

    companion object {

        val WORK_DIR_NAME = "gap"

        val ARG_SAVE_PROPS = "--save-props"

        val ARG_SAVE_PREFIX = "-P"

        val ARGS = listOf(ARG_SAVE_PROPS)

        @JvmStatic
        fun main(args: Array<String>) {
            val saveProps = args.contains(ARG_SAVE_PROPS)
            val gradleArgs = args.filterNot { ARGS.contains(it) }
            val buildConfig = Properties().apply {
                load(Launcher::class.java.getResourceAsStream("/build.properties"))
            }
            val gradleVersion = buildConfig["gradleVersion"]?.toString()
                    ?: throw LauncherException("Gradle version info not available!")
            val pluginVersion = buildConfig["pluginVersion"]?.toString()
                    ?: throw LauncherException("AEM Plugin version info not available!")

            val currentDir = File(".")
            val workDir = currentDir.resolve(WORK_DIR_NAME)

            if (saveProps) {
                saveProperties(workDir, gradleArgs)
            }
            saveSettings(workDir)
            saveBuildScript(workDir, pluginVersion)

            runBuild(workDir, gradleArgs, gradleVersion)
        }

        private fun saveProperties(workDir: File, args: List<String>) {
            workDir.resolve("gradle.properties").apply {
                if (exists()) return@apply
                parentFile.mkdirs()

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

        private fun saveBuildScript(workDir: File, pluginVersion: String?) {
            workDir.resolve("build.gradle.kts").apply {
                if (exists()) return@apply
                parentFile.mkdirs()

                writeText("""
                            plugins {
                                id("com.cognifide.aem.instance.local") version "$pluginVersion"
                                id("com.cognifide.aem.package.sync") version "$pluginVersion"
                            }
                        """.trimIndent())
            }
        }

        private fun saveSettings(workDir: File) {
            workDir.resolve("settings.gradle.kts").apply {
                if (exists()) return@apply
                parentFile.mkdirs()

                writeText("""
                            pluginManagement {
                                repositories {
                                    mavenLocal()
                                    jcenter()
                                    gradlePluginPortal()
                                }
                            }
                            
                            rootProject.name = "$WORK_DIR_NAME"
                        """.trimIndent())
            }
        }

        private fun runBuild(workDir: File, args: List<String>, gradleVersion: String?): Unit = try {
            GradleConnector.newConnector()
                    .useGradleVersion(gradleVersion)
                    .forProjectDirectory(workDir)
                    .connect().use { connection ->
                        connection.newBuild()
                                .withArguments(args)
                                .setColorOutput(true)
                                .setStandardOutput(System.out)
                                .setStandardError(System.err)
                                .setStandardInput(System.`in`)
                                .run()
                    }
            exitProcess(0)
        } catch (e: Exception) {
            e.printStackTrace(System.err)
            exitProcess(1)
        }
    }
}

