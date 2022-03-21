package com.cognifide.gradle.aem.common
import com.cognifide.gradle.aem.test.AemBuildTest
import org.junit.jupiter.api.Test

class CommonPluginTest : AemBuildTest() {

    @Test
    fun `should apply plugin correctly`() {
        val projectDir = prepareProject("common-minimal") {
            settingsGradle("")

            buildGradle(
                """
                plugins {
                    id("com.cognifide.aem.common")
                }
                
                aem {
                    // anything
                }
                """
            )
        }

        runBuild(projectDir, "tasks") {
            assertTask(":tasks")
        }
    }
}
