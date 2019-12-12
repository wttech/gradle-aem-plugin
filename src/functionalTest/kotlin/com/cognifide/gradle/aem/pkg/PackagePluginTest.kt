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
    fun `should build package using minimal configuration`() {
        // given
        val projectDir = File("build/functionalTest/minimal").apply {
            mkdirs()

            resolve("settings.gradle.kts").writeText("")

            resolve("build.gradle.kts").writeText("""
                plugins {
                    id("com.cognifide.aem.package")
                }
                """.trimIndent())

            resolve("src/main/content/jcr_root/apps/example/.content.xml").apply {
                parentFile.mkdirs()
            }.writeText("""
                <?xml version="1.0" encoding="UTF-8"?>
                <jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0" xmlns:jcr="http://www.jcp.org/jcr/1.0"
                    jcr:primaryType="sling:Folder"/>
                """.trimIndent())

            resolve("src/main/content/META-INF/vault/filter.xml").apply {
                parentFile.mkdirs()
            }.writeText("""
                <?xml version="1.0" encoding="UTF-8"?>
                <workspaceFilter version="1.0">
                    <filter root="/apps/example/common"/>
                </workspaceFilter>
                """.trimIndent())
        }

        // when
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("packageCompose", "-Poffline")
        runner.withProjectDir(projectDir)
        val result = runner.build();

        // then
        assertEquals(result.task(":packageCompose")?.outcome, TaskOutcome.SUCCESS)
    }
}