package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.test.AemBuildTest
import org.gradle.internal.impldep.org.testng.AssertJUnit.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty

class LocalInstancePluginTest : AemBuildTest() {

    @Test
    fun `should apply plugin correctly`() {
        val projectDir = prepareProject("local-instance-minimal") {
            settingsGradle("")

            buildGradle("""
                plugins {
                    id("com.cognifide.aem.instance.local")
                }
                """)
        }

        runBuild(projectDir, "tasks", "-Poffline") {
            assertTask(":tasks")
        }
    }


    @EnabledIfSystemProperty(named = "localInstance.jarUrl", matches = ".+")
    @Test
    fun `should setup local aem author and publish instances`() {
        val projectDir = prepareProject("local-instance-setup") {
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
                
                localInstance.backup.uploadUrl=build/backups-upload
                """)

            settingsGradle("")

            buildGradle("""
                plugins {
                    id("com.cognifide.aem.instance.local")
                }
                
                repositories {
                    jcenter()
                    maven { url = uri("https://repo.adobe.com/nexus/content/groups/public") }
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
                        
                        register("assertIfCrxDeEnabled") {
                            doLast {
                                sync {
                                    osgiFramework {
                                        val crxDe = getConfiguration("org.apache.sling.jcr.davex.impl.servlets.SlingDavExServlet")
                                        if (crxDe.properties["alias"] != "/crx/server") {
                                            throw Exception("CRX/DE is not enabled on ${'$'}instance!!")
                                        }
                                    }
                                }
                            }
                            mustRunAfter("instanceSetup")
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
            assertFileExists(file(".instance/author"))
            assertFileExists(file(".instance/publish"))
        }

        runBuild(projectDir, "instanceDown") {
            assertTask(":instanceDown")
        }

        runBuild(projectDir, "instanceBackup") {
            assertTask(":instanceBackup")

            val localBackupDir = "build/instanceBackup/local"
            assertFileExists(localBackupDir)
            val localBackups = file(localBackupDir).walk().filter { it.name.endsWith(".backup.zip") }.toList()
            assertEquals("Backup file should end with *.backup.zip suffix!",
                    1, localBackups.count())

            val remoteBackupDir = "build/backups-upload"
            assertFileExists(remoteBackupDir)
            val remoteBackups = file(remoteBackupDir).walk().filter { it.name.endsWith(".backup.zip") }.toList()
            assertEquals("Backup file should end with *.backup.zip suffix!",
                    1, remoteBackups.count())

            val localBackup = localBackups.first()
            val remoteBackup = remoteBackups.first()
            assertEquals("Local & remote backup names does not match!",
                    localBackup.name, remoteBackup.name)
            assertEquals("Local & remote backup size does not match!",
                    localBackup.length(), remoteBackup.length())
        }

        runBuild(projectDir, "instanceDestroy", "-Pforce") {
            assertTask(":instanceDown")
            assertTask(":instanceDestroy")
            assertFileNotExists(".instance/author")
            assertFileNotExists(".instance/publish")
        }

        runBuild(projectDir, "instanceUp") {
            assertTask(":instanceCreate")
            assertTask(":instanceUp")
            assertFileExists(".instance/author")
            assertFileExists(".instance/publish")
        }

        runBuild(projectDir, "assertIfCrxDeEnabled") {
            assertTask(":assertIfCrxDeEnabled")
        }

        runBuild(projectDir, "instanceResetup", "-Pforce", "assertIfCrxDeEnabled") {
            assertTask(":instanceDown")
            assertTask(":instanceDestroy")
            assertTask(":instanceCreate")
            assertTask(":instanceUp")
            assertTask(":instanceSatisfy")
            assertTask(":instanceProvision")
            assertTask(":instanceSetup")
            assertTask(":instanceResetup ")
            assertTask(":assertIfCrxDeEnabled")
            assertFileExists(".instance/author")
            assertFileExists(".instance/publish")
        }

        runBuild(projectDir, "instanceDestroy", "-Pforce") {
            assertTask(":instanceDestroy")
        }

        runBuild(projectDir, "clean") {
            assertTask(":clean")
        }
    }
}