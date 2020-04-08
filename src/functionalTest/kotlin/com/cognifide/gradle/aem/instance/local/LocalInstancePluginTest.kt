package com.cognifide.gradle.aem.instance.local

import com.cognifide.gradle.aem.test.AemBuildTest
import org.gradle.internal.impldep.org.testng.AssertJUnit.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty

@Suppress("LongMethod", "MaxLineLength")
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

    @Test
    fun `should resolve instance files properly`() {
        val projectDir = prepareProject("instance-resolve") {
            settingsGradle("")

            file("src/aem/files/cq-quickstart-6.5.0.jar", "")
            file("src/aem/files/license.properties", "")

            gradleProperties("""
                localInstance.quickstart.jarUrl=src/aem/files/cq-quickstart-6.5.0.jar
                localInstance.quickstart.licenseUrl=src/aem/files/license.properties
            """)

            buildGradle("""
                plugins {
                    id("com.cognifide.aem.instance.local")
                }
                
                aem {
                    instance {
                        satisfier {
                            packages {
                               "dep.ac-tool"(
                                   "https://repo1.maven.org/maven2/biz/netcentric/cq/tools/accesscontroltool/accesscontroltool-package/2.3.2/accesscontroltool-package-2.3.2.zip", 
                                   "https://repo1.maven.org/maven2/biz/netcentric/cq/tools/accesscontroltool/accesscontroltool-oakindex-package/2.3.2/accesscontroltool-oakindex-package-2.3.2.zip"
                                )
                               "dep.search-webconsole-plugin"("com.neva.felix:search-webconsole-plugin:1.3.0")
                               "tool.aem-groovy-console"("https://github.com/icfnext/aem-groovy-console/releases/download/14.0.0/aem-groovy-console-14.0.0.zip")
                            }
                        }
                    }
                }
                """)
        }

        runBuild(projectDir, "instanceResolve", "-Poffline") {
            assertTask(":instanceResolve")

            assertFileExists("build/instance/quickstart/cq-quickstart-6.5.0.jar")
            assertFileExists("build/instance/quickstart/license.properties")

            assertFileExists("build/instance/satisfy/packages/4f135495/aem-groovy-console-14.0.0.zip")
            assertFileExists("build/instance/satisfy/packages/f30506c4/accesscontroltool-package-2.3.2.zip")
            assertFileExists("build/instance/satisfy/packages/6182d096/accesscontroltool-oakindex-package-2.3.2.zip")
            assertPackage("build/instance/satisfy/packages/df0bbfa8/search-webconsole-plugin-1.3.0.zip")
        }
    }

    @EnabledIfSystemProperty(named = "localInstance.quickstart.jarUrl", matches = ".+")
    @Test
    fun `should setup and backup local aem author and publish instances`() {
        val projectDir = prepareProject("local-instance-setup-and-backup") {
            gradleProperties("""
                fileTransfer.user=${System.getProperty("fileTransfer.user")}
                fileTransfer.password=${System.getProperty("fileTransfer.password")}
                fileTransfer.domain=${System.getProperty("fileTransfer.domain")}
                
                localInstance.quickstart.jarUrl=${System.getProperty("localInstance.quickstart.jarUrl")}
                localInstance.quickstart.licenseUrl=${System.getProperty("localInstance.quickstart.licenseUrl")}
                
                instance.local-author.httpUrl=http://localhost:9502
                instance.local-author.type=local
                instance.local-author.runModes=local,nosamplecontent
                instance.local-author.jvmOpts=-server -Xmx2048m -XX:MaxPermSize=512M -Djava.awt.headless=true

                instance.local-publish.httpUrl=http://localhost:9503
                instance.local-publish.type=local
                instance.local-publish.runModes=local,nosamplecontent
                instance.local-publish.jvmOpts=-server -Xmx2048m -XX:MaxPermSize=512M -Djava.awt.headless=true
                
                localInstance.backup.uploadUrl=build/localInstance/backup/upload
                """)

            settingsGradle("")

            buildGradle("""
                plugins {
                    id("com.cognifide.aem.instance.local")
                }
                
                repositories {
                    maven("https://repo.adobe.com/nexus/content/groups/public")
                }
                
                aem {
                    instance {
                        satisfier {
                            packages {
                                "dep.core-components-all"("com.adobe.cq:core.wcm.components.all:2.8.0@zip")
                                "tool.search-webconsole-plugin"("com.neva.felix:search-webconsole-plugin:1.2.0")
                            }
                        }
                        provisioner {
                            step("enable-crxde") {
                                description = "Enables CRX DE"
                                condition { once() && instance.env != "prod" }
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
                    
                    tasks {
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

        runBuild(projectDir, "instanceStatus") {
            assertTask(":instanceStatus")
        }

        runBuild(projectDir, "instanceSetup") {
            assertTask(":instanceSetup")
            assertFileExists(file(".instance/author"))
            assertFileExists(file(".instance/publish"))
        }

        runBuild(projectDir, "instanceStatus") {
            assertTask(":instanceStatus")
        }

        runBuild(projectDir, "instanceDown") {
            assertTask(":instanceDown")
        }

        runBuild(projectDir, "instanceBackup") {
            assertTask(":instanceBackup")

            val localBackupDir = "build/localInstance/backup/local"
            assertFileExists(localBackupDir)
            val localBackups = files(localBackupDir, "**/*.backup.zip")
            assertEquals("Backup file should end with *.backup.zip suffix!",
                    1, localBackups.count())

            val remoteBackupDir = "build/localInstance/backup/upload"
            assertFileExists(remoteBackupDir)
            val remoteBackups = files(remoteBackupDir, "**/*.backup.zip")
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

        runBuild(projectDir, "instanceDestroy", "-Pforce") {
            assertTask(":instanceDestroy")
        }

        runBuild(projectDir, "clean") {
            assertTask(":clean")
        }
    }

    @EnabledIfSystemProperty(named = "localInstance.quickstart.jarUrl", matches = ".+")
    @Test
    fun `should repeat setup of local aem author and publish instances`() {
        val projectDir = prepareProject("local-instance-resetup") {
            gradleProperties("""
                fileTransfer.user=${System.getProperty("fileTransfer.user")}
                fileTransfer.password=${System.getProperty("fileTransfer.password")}
                fileTransfer.domain=${System.getProperty("fileTransfer.domain")}
                
                localInstance.quickstart.jarUrl=${System.getProperty("localInstance.quickstart.jarUrl")}
                localInstance.quickstart.licenseUrl=${System.getProperty("localInstance.quickstart.licenseUrl")}
                
                instance.local-author.httpUrl=http://localhost:9502
                instance.local-author.type=local
                instance.local-author.runModes=local,nosamplecontent
                instance.local-author.jvmOpts=-server -Xmx2048m -XX:MaxPermSize=512M -Djava.awt.headless=true
                """)

            settingsGradle("")

            buildGradle("""
                plugins {
                    id("com.cognifide.aem.instance.local")
                }
                """)
        }

        runBuild(projectDir, "instanceResetup", "-Pforce") {
            assertTask(":instanceDown")
            assertTask(":instanceDestroy")
            assertTask(":instanceCreate")
            assertTask(":instanceUp")
            assertTask(":instanceSatisfy")
            assertTask(":instanceProvision")
            assertTask(":instanceSetup")
            assertTask(":instanceResetup")
            assertFileExists(".instance/author")
            assertFileNotExists(".instance/publish")
        }

        runBuild(projectDir, "instanceDestroy", "-Pforce") {
            assertTask(":instanceDestroy")
        }

        runBuild(projectDir, "clean") {
            assertTask(":clean")
        }
    }
}
