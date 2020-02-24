package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.test.AemBuildTest
import org.junit.jupiter.api.Test

class InstancePluginTest : AemBuildTest() {

    @Test
    fun `should apply plugin correctly`() {
        val projectDir = prepareProject("instance-minimal") {
            settingsGradle("")

            buildGradle("""
                plugins {
                    id("com.cognifide.aem.instance")
                }
                """)
        }

        runBuild(projectDir, "tasks", "-Poffline") {
            assertTask(":tasks")
        }
    }
}