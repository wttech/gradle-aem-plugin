package com.cognifide.gradle.aem.pkg
import com.cognifide.gradle.aem.test.AemBuildTest
import org.junit.jupiter.api.Test

class PackageSyncPluginTest: AemBuildTest() {

    @Test
    fun `should apply plugin correctly`() {
        val projectDir = prepareProject("package-sync-minimal") {
            settingsGradle("")

            buildGradle("""
                plugins {
                    id("com.cognifide.aem.package.sync")
                }
                """)
        }

        runBuild(projectDir, "tasks", "-Poffline") {
            assertTask(":tasks")
        }
    }
}