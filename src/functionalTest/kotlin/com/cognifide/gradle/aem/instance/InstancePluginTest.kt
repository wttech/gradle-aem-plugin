package com.cognifide.gradle.aem.instance
import com.cognifide.gradle.aem.test.AemBuildTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty

class InstancePluginTest: AemBuildTest() {

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
            assertTask( ":tasks")
        }
    }

    @EnabledIfSystemProperty(named = "localInstance.jarUrl", matches = ".+")
    @Test
    fun `should setup local aem author and publish instances`() {
        val projectDir = prepareProject("instance-setup") {
            gradleProperties("""
                fileTransfer.user=${System.getProperty("fileTransfer.user")}
                fileTransfer.password=${System.getProperty("fileTransfer.password")}
                fileTransfer.domain=${System.getProperty("fileTransfer.domain")}
                
                localInstance.quickstart.jarUrl=${System.getProperty("localInstance.jarUrl")}
                localInstance.quickstart.licenseUrl=${System.getProperty("localInstance.licenseUrl")}
                
                instance.local-author.httpUrl=http://localhost:9502
                instance.local-author.type=local
                instance.local-author.runModes=local,nosamplecontent
                instance.local-author.jvmOpts=-server -Xmx2048m -XX:MaxPermSize=512M -Djava.awt.headless=true

                instance.local-publish.httpUrl=http://localhost:9503
                instance.local-publish.type=local
                instance.local-publish.runModes=local,nosamplecontent
                instance.local-publish.jvmOpts=-server -Xmx2048m -XX:MaxPermSize=512M -Djava.awt.headless=true
                """)

            settingsGradle("")

            buildGradle("""
                plugins {
                    id("com.cognifide.aem.instance")
                }
                
                repositories {
                    jcenter()
                    maven { url = uri("https://repo.adobe.com/nexus/content/groups/public") }
                    maven { url = uri("https://dl.bintray.com/neva-dev/maven-public") }
                }
                
                aem {
                    tasks {
                        instanceSatisfy {
                            packages {
                                "dep.core-components-all"("com.adobe.cq:core.wcm.components.all:2.8.0@zip")
                                "tool.search-webconsole-plugin"("com.neva.felix:search-webconsole-plugin:1.2.0")
                            }
                        }
                
                        instanceProvision {
                            step("enable-crxde") {
                                description = "Enables CRX DE"
                                condition { once() && instance.environment != "prod" }
                                action {
                                    sync {
                                        osgiFramework.configure("org.apache.sling.jcr.davex.impl.servlets.SlingDavExServlet", mapOf(
                                                "alias" to "/crx/server"
                                        ))
                                    }
                                }
                            }
                        }
                    }
                }
                """)
        }

        runBuild(projectDir, "instanceResolve") {
            assertTask(":instanceResolve")
        }

        runBuild(projectDir, "instanceSetup") {
            assertTask(":instanceSetup")
        }

        runBuild(projectDir, "instanceDestroy", "-Pforce") {
            assertTask(":instanceDestroy")
        }
    }
}