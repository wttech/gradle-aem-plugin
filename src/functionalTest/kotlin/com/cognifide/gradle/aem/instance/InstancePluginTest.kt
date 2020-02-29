package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.test.AemBuildTest
import org.junit.jupiter.api.Test

class InstancePluginTest : AemBuildTest() {

    @Test
    fun `should apply plugin correctly`() {
        val projectDir = prepareProject("instance-minimal") {
            settingsGradle("")

            buildGradle("""
                plugins {
                    id("com.cognifide.aem.instance")
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
                    id("com.cognifide.aem.instance")
                }
                
                aem {
                    instance {
                        provisioner {
                            step("enable-crxde") {
                                description = "Enables CRX DE"
                                condition { once() && instance.environment != "prod" }
                                sync {
                                    osgiFramework.configure("org.apache.sling.jcr.davex.impl.servlets.SlingDavExServlet", mapOf(
                                            "alias" to "/crx/server"
                                    ))
                                }
                            }
                            step("setup-replication-author") {
                                condition { once() && instance.author }
                                sync {
                                    repository {
                                        save("/etc/replication/agents.author/publish/jcr:content", mapOf(
                                                "enabled" to true,
                                                "userId" to instance.user,
                                                "transportUri" to "http://localhost:4503/bin/receive?sling:authRequestLogin=1",
                                                "transportUser" to instance.user,
                                                "transportPassword" to instance.password
                                        ))
                                    }
                                }
                            }
                            step("disable-unsecure-bundles") {
                                condition { once() && instance.environment == "prod" }
                                sync {
                                    osgiFramework.stopBundle("org.apache.sling.jcr.webdav")
                                    osgiFramework.stopBundle("com.adobe.granite.crxde-lite")
                
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
