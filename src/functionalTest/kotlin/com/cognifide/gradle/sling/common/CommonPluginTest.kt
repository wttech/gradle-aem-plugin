package com.cognifide.gradle.sling.common
import com.cognifide.gradle.sling.test.SlingBuildTest
import org.junit.jupiter.api.Test

class CommonPluginTest : SlingBuildTest() {

    @Test
    fun `should apply plugin correctly`() {
        val projectDir = prepareProject("common-minimal") {
            settingsGradle("")

            buildGradle("""
                plugins {
                    id("com.cognifide.sling.common")
                }
                
                sling {
                    // anything
                }
                """)
        }

        runBuild(projectDir, "tasks") {
            assertTask(":tasks")
        }
    }
}
