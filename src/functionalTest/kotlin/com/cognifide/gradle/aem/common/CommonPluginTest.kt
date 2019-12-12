package com.cognifide.gradle.aem.common
import com.cognifide.gradle.aem.test.BaseTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CommonPluginTest: BaseTest() {

    @Test
    fun `should debug aem configuration`() {
        // given
        val projectDir = projectDir("common/debug") {
            settingsGradle("")

            buildGradle("""
                plugins {
                    id("com.cognifide.aem.common")
                }
                """)
        }

        // when
        val buildResult = runBuild(projectDir, "debug", "-Poffline")

        // then
        assertTask(buildResult, ":debug")
        assertTrue(projectDir.resolve("build/aem/debug/debug.json").exists())
    }
}