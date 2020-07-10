package com.cognifide.gradle.sling.instance

import com.cognifide.gradle.sling.test.SlingBuildTest
import org.junit.jupiter.api.Test

class InstancePluginTest : SlingBuildTest() {

    @Test
    fun `should apply plugin correctly`() {
        val projectDir = prepareProject("instance-minimal") {
            settingsGradle("")

            buildGradle("""
                plugins {
                    id("com.cognifide.sling.instance")
                }
                """)
        }

        runBuild(projectDir, "tasks", "-Poffline") {
            assertTask(":tasks")
        }
    }

    @Test
    fun `should define provisioner steps properly`() {
        val projectDir = prepareProject("instance-provisioner") {
            settingsGradle("")

            buildGradle("""
                plugins {
                    id("com.cognifide.sling.instance")
                }
                
                sling {
                    instance {
                        provisioner {
                            step("disable-unsecure-bundles") {
                                condition { once() && instance.env == "prod" }
                                sync {
                                    osgiFramework.stopBundle("org.apache.sling.jcr.webdav")
                                    instance.awaitUp() // include above in property: 'instance.awaitUp.bundles.symbolicNamesIgnored'
                                }
                            }
                        }
                    }
                }
                """)
        }

        runBuild(projectDir, "tasks", "-Poffline") {
            assertTask(":tasks")
        }
    }
}
