package com.cognifide.gradle.aem.common
import com.cognifide.gradle.aem.test.AemBuildTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CommonPluginTest: AemBuildTest() {

    @Test
    fun `should debug aem configuration`() {
        val projectDir = prepareProject("common-debug") {
            settingsGradle("")

            buildGradle("""
                plugins {
                    id("com.cognifide.aem.common")
                }
                """)
        }

        runBuild(projectDir, "debug", "-Poffline") {
            assertTask(":debug")
            assertFileExists("build/aem/debug/debug.json")
        }
    }
}