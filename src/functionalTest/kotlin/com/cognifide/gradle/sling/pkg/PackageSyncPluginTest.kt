package com.cognifide.gradle.sling.pkg
import com.cognifide.gradle.sling.test.SlingBuildTest
import org.junit.jupiter.api.Test

class PackageSyncPluginTest : SlingBuildTest() {

    @Test
    fun `should apply plugin correctly`() {
        val projectDir = prepareProject("package-sync-minimal") {
            settingsGradle("")

            buildGradle("""
                plugins {
                    id("com.cognifide.sling.package.sync")
                }
                """)
        }

        runBuild(projectDir, "tasks", "-Poffline") {
            assertTask(":tasks")
        }
    }
}
