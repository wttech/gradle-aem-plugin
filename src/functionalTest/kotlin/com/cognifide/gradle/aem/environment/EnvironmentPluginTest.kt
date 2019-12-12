package com.cognifide.gradle.aem.environment
import com.cognifide.gradle.aem.test.BaseTest
import org.junit.jupiter.api.Test

class EnvironmentPluginTest: BaseTest() {

    @Test
    fun `should apply plugin correctly`() {
        // given
        val projectDir = projectDir("environment/minimal") {
            settingsGradle("")

            buildGradle("""
                plugins {
                    id("com.cognifide.aem.environment")
                }
                """)
        }

        // when
        val buildResult = runBuild(projectDir, "tasks", "-Poffline")

        // then
        assertTask(buildResult, ":tasks")
    }
}