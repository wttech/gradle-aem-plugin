package com.cognifide.gradle.sling

import com.cognifide.gradle.sling.test.SlingBuildTest
import org.junit.jupiter.api.Test

class PluginsTest : SlingBuildTest() {

    @Test
    fun `should apply all plugins at once`() {
        val projectDir = prepareProject("plugins-all") {
            settingsGradle("")

            buildGradle("""
                plugins {
                    id("com.cognifide.sling.instance.local")
                    id("com.cognifide.sling.bundle")
                    id("com.cognifide.sling.package")
                    id("com.cognifide.sling.package.sync")
                }
                
                sling {
                    // ...
                }
                """)
        }

        runBuild(projectDir, "tasks", "-Poffline") {
            assertTask(":tasks")
        }
    }
}
