package com.cognifide.gradle.aem.launcher

import org.gradle.tooling.GradleConnector
import java.io.File
import kotlin.system.exitProcess

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

                resolve("buildSrc/build.gradle.kts").apply {
                    parentFile.mkdirs()

                    writeText("""
                        dependencies {
                            implementation(files("../libs/gap.jar"))
                        }
                    """.trimIndent())
                }

                resolve("settings.gradle.kts").writeText("""
                    rootProject.name = "gap"
                """.trimIndent())

                resolve("build.gradle.kts").writeText("""
                    plugins {
                        id("com.cognifide.aem.instance.local")
                        id("com.cognifide.aem.package.sync")
                    }
                """.trimIndent())
            }

            try {
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
                exitProcess(0)
            } catch (e: Exception) {
                e.printStackTrace(System.err)
                exitProcess(1)
            }
        }
    }
}

