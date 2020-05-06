package com.cognifide.gradle.aem.launcher

import org.gradle.tooling.GradleConnector
import java.io.File

class Launcher {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val currentDir = File(".")
            val workDir = currentDir.resolve("gap").apply {
                mkdirs()

                resolve("libs").apply {
                    mkdirs()

                    resolve("gap.jar").outputStream().use { output ->
                        Launcher::class.java.getResourceAsStream("/gap.jar").let { input ->
                            input.copyTo(output)
                        }
                    }
                }

                resolve("build.gradle.kts").writeText("""
                    buildscript {
                        dependencies.classpath(fileTree("libs"))
                    }
                    
                    plugins {
                        id("com.cognifide.aem.instance.local")
                        id("com.cognifide.aem.package.sync")
                    }
                """.trimIndent())

                resolve("settings.gradle.kts").writeText("""
                    rootProject.name = "gap"
                """.trimIndent())
            }

            GradleConnector.newConnector()
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
        }
    }
}

