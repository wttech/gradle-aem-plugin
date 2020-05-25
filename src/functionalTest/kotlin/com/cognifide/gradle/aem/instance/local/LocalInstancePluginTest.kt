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
                        provisioner {
                            deployPackage("https://repo1.maven.org/maven2/biz/netcentric/cq/tools/accesscontroltool/accesscontroltool-package/2.3.2/accesscontroltool-package-2.3.2.zip")
                            deployPackage("https://repo1.maven.org/maven2/biz/netcentric/cq/tools/accesscontroltool/accesscontroltool-oakindex-package/2.3.2/accesscontroltool-oakindex-package-2.3.2.zip")
                            deployPackage("com.neva.felix:search-webconsole-plugin:1.3.0")
                            deployPackage("https://github.com/icfnext/aem-groovy-console/releases/download/14.0.0/aem-groovy-console-14.0.0.zip")
                        }
                    }
                }
                """)
        }

        runBuild(projectDir, "instanceResolve", "-Poffline") {
            assertTask(":instanceResolve")

            assertFileExists("build/instance/quickstart/cq-quickstart-6.5.0.jar")
            assertFileExists("build/instance/quickstart/license.properties")

            assertFileExists("build/instance/provision/files/4f135495/aem-groovy-console-14.0.0.zip")
            assertFileExists("build/instance/provision/files/6182d096/accesscontroltool-oakindex-package-2.3.2.zip")
            assertFileExists("build/instance/provision/files/f30506c4/accesscontroltool-package-2.3.2.zip")
            assertPackage("build/package/wrapper/search-webconsole-plugin-1.3.0.zip")
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
                
                instance.local-author.httpUrl=http://localhost:8802
                instance.local-author.type=local
                instance.local-author.runModes=local,nosamplecontent
                instance.local-author.jvmOpts=-server -Xmx2048m -XX:MaxPermSize=512M -Djava.awt.headless=true

                instance.local-publish.httpUrl=http://localhost:8803
                instance.local-publish.type=local
                instance.local-publish.runModes=local,nosamplecontent
                instance.local-publish.jvmOpts=-server -Xmx2048m -XX:MaxPermSize=512M -Djava.awt.headless=true
                
                localInstance.backup.localDir=$BACKUP_DIR/local
                localInstance.backup.uploadUrl=$BACKUP_DIR/upload
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
                        provisioner {
                            enableCrxDe()
                            deployPackage("com.adobe.cq:core.wcm.components.all:2.8.0@zip")
                            deployPackage("com.neva.felix:search-webconsole-plugin:1.2.0")
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

        try {
            runBuild(projectDir, "instanceStatus") {
                assertTask(":instanceStatus")
            }

            runBuild(projectDir, "instanceSetup") {
                assertTask(":instanceSetup")
                assertFileExists(file(".gradle/aem/localInstance/instance/author"))
                assertFileExists(file(".gradle/aem/localInstance/instance/publish"))
            }

            runBuild(projectDir, "instanceStatus") {
                assertTask(":instanceStatus")
            }

            runBuild(projectDir, "instanceDown") {
                assertTask(":instanceDown")
            }

            runBuild(projectDir, "instanceBackup") {
                assertTask(":instanceBackup")

                val localBackupDir = "$BACKUP_DIR/local"
                assertFileExists(localBackupDir)
                val localBackups = files(localBackupDir, "**/*.backup.zip")
                assertEquals("Backup file should end with *.backup.zip suffix!",
                        1, localBackups.count())

                val remoteBackupDir = "$BACKUP_DIR/upload"
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
                assertFileNotExists(".gradle/aem/localInstance/instance/author")
                assertFileNotExists(".gradle/aem/localInstance/instance/publish")
            }

            runBuild(projectDir, "instanceUp") {
                assertTask(":instanceCreate")
                assertTask(":instanceUp")
                assertFileExists(".gradle/aem/localInstance/instance/author")
                assertFileExists(".gradle/aem/localInstance/instance/publish")
            }

            runBuild(projectDir, "assertIfCrxDeEnabled") {
                assertTask(":assertIfCrxDeEnabled")
            }

            runBuild(projectDir, "instanceDestroy", "-Pforce") {
                assertTask(":instanceDestroy")
            }
        } finally {
            runBuild(projectDir, "instanceKill") {
                assertTask(":instanceKill")
            }
        }
    }

    companion object {
        const val BACKUP_DIR = ".gradle/aem/localInstance/backup"
    }
}
