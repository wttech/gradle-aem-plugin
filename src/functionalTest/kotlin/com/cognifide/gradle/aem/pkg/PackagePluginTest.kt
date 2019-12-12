package com.cognifide.gradle.aem.pkg

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled

import org.junit.jupiter.api.Test
import java.io.File

class PackagePluginTest {

    @Test
    fun helloWorld() {
        println(File("./").absoluteFile)
    }

    @Test
    @Disabled
    fun `can run default suite`() {
        // given
        val projectDir = configureProjectDir()

        // when
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("lighthouseRun")
        runner.withProjectDir(projectDir)
        val result = runner.build();

        // then
        assertEquals(result.task(":lighthouseRun")?.outcome, TaskOutcome.SUCCESS)
    }

    private fun configureProjectDir(): File {
        val projectDir = File("build/functionalTest")

        with(projectDir) {
            mkdirs()
            resolve("settings.gradle.kts").writeText("")
            resolve("build.gradle.kts").writeText("""
                plugins {
                    id("com.cognifide.lighthouse")
                }
                """.trimIndent())

            resolve("lighthouse").mkdirs()

            resolve("lighthouse/suites.json").writeText("""
                {
                  "suites": [
                    {
                      "name": "youtube",
                      "default": true,
                      "baseUrl": "https://www.youtube.com",
                      "paths": [
                        "/",
                        "/feed/trending"
                      ],
                      "args": [
                        "--config-path=lighthouse/config.json",
                        "--performance=60",
                        "--accessibility=70",
                        "--best-practices=80",
                        "--seo=80",
                        "--pwa=30"
                      ]
                    },
                    {
                      "name": "facebook",
                      "baseUrl": "https://www.facebook.com",
                      "paths": [
                        "/"
                      ],
                      "args": [
                        "--config-path=lighthouse/config.json",
                        "--performance=75",
                        "--accessibility=60",
                        "--best-practices=80",
                        "--seo=60",
                        "--pwa=30"
                      ]
                    }
                  ]
                }
                """.trimIndent())

            resolve("lighthouse/config.json").writeText("""
                {
                  "extends": "lighthouse:default"
                }
                """.trimIndent())
        }
        return projectDir
    }
}