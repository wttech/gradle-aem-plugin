package com.cognifide.gradle.aem.launcher

import org.gradle.tooling.GradleConnector
import java.io.File
import kotlin.system.exitProcess

class Launcher {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val gradleVersion = "6.4"
            val gapVersion = "13.1.1"

            val currentDir = File(".")
            val workDir = currentDir.resolve("gap").apply {
                mkdirs()

                resolve("settings.gradle.kts").writeText("""
                    pluginManagement {
                        repositories {
                            mavenLocal()
                            jcenter()
                            gradlePluginPortal()
                        }
                    }
                    
                    rootProject.name = "gap"
                """.trimIndent())

                resolve("build.gradle.kts").writeText("""
                    plugins {
                        id("com.cognifide.aem.instance.local") version "$gapVersion"
                        id("com.cognifide.aem.package.sync") version "$gapVersion"
                    }
                """.trimIndent())
            }

            try {
                GradleConnector.newConnector()
                        .useGradleVersion(gradleVersion)
                        .forProjectDirectory(workDir)
                        .connect().use { connection ->
                            connection.newBuild()
                                    .withArguments(args.asIterable())
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
}

