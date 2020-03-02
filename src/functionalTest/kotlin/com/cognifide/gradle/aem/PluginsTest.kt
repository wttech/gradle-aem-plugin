package com.cognifide.gradle.aem

import com.cognifide.gradle.aem.test.AemBuildTest
import org.junit.jupiter.api.Test

class PluginsTest : AemBuildTest() {

    @Test
    fun `should apply all plugins at once`() {
        val projectDir = prepareProject("plugins-all") {
            settingsGradle("")

            buildGradle("""
                plugins {
                    id("com.cognifide.aem.instance.local")
                    id("com.cognifide.aem.bundle")
                    id("com.cognifide.aem.package")
                    id("com.cognifide.aem.package.sync")
                }
                
                aem {
                    // ...
                }
                """)
        }

        runBuild(projectDir, "tasks", "-Poffline") {
            assertTask(":tasks")
        }
    }
}
