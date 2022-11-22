package com.cognifide.gradle.aem.launcher

object EnvCommonScaffolder {

    @Suppress("LongMethod", "MaxLineLength")
    fun saveRootBuildScript(launcher: Launcher) {
        launcher.workFileOnce("build.gradle.kts") {
            println("Saving root Gradle build script file '$this'")

            val mavenRootDir = when {
                launcher.appDirPath.isBlank() -> ""
                else -> """rootDir("${launcher.appDirPath}")"""
            }

            writeText(
                """
                import com.cognifide.gradle.aem.common.instance.local.OpenMode
                    
                plugins {
                    id("io.wttech.config")
                    id("com.cognifide.aem.common")
                }
                
                config {
                    define {
                        label("AEM Configuration")
                        
                        valueSaveGradleProperties()
                        labelAbbrs("aem")
                    
                        group("aemInstance") {
                            prop("aemInstanceType") {
                                options("local", "remote")
                                description("Local - instance will be created on local file system\nRemote - connecting to remote instance only")
                            }
                            prop("aemInstancePassword") {
                                default("admin")
                                description("Needed to access AEM admin (author & publish)")
                                required()
                            }
                            prop("aemAuthorEnabled") { checkbox(true) }
                            prop("aemAuthorHttpUrl") {
                                visible { boolValue("aemAuthorEnabled") }
                                enabled { stringValue("aemInstanceType") == "remote" }
                                default("http://127.0.0.1:4502")
                                optional()
                            }
                            prop("aemPublishEnabled") { checkbox(true) }
                            prop("aemPublishHttpUrl") {
                                visible { boolValue("aemPublishEnabled") }
                                enabled { stringValue("aemInstanceType") == "remote" }
                                default("http://127.0.0.1:4503")
                                optional()
                            }
                            prop("aemFlushHttpUrl") {
                                visible { stringValue("aemInstanceType") == "local" }
                                description("URL for flushing AEM dispatcher cache")
                                default("http://127.0.0.1:80")
                                disabled()
                            }
                            prop("aemInstanceRunMode") {
                                visible { stringValue("aemInstanceType") == "local" }
                                options("local", "dev", "stage", "prod")
                            }
                            prop("aemInstanceServiceCredentialsUrl") {
                                visible { stringValue("aemInstanceType") == "remote" && common.patterns.wildcard(stringValue("aemAuthorHttpUrl"), "*.adobeaemcloud.com") }
                                description("JSON file downloaded from AEMaaCS Developer Console")
                                optional()
                            }
                            prop("aemInstanceOpenMode") {
                                visible { stringValue("aemInstanceType") == "local" }
                                description("Open web browser when instances are up")
                                options(OpenMode.values().map { it.name.toLowerCase() })
                                default(OpenMode.NEVER.name.toLowerCase())
                            }
                            
                            // ---
                            
                            prop("aemQuickstartDistUrl") {
                                label("AEM distribution URL")
                                description("Typically AEM SDK zip file or AEM jar file")
                            }
                            prop("aemQuickstartLicenseUrl") {
                                label("Quickstart License URL")
                                description("Typically file named 'license.properties'")
                            }
                            prop("aemServicePackUrl") {
                                description("Typically file named 'aem-service-pkg-*.zip'")
                                optional()
                            }
                            prop("aemCoreComponentsUrl") {
                                description("Typically file named 'core.wcm.components.all-*.zip'")
                                optional()
                            }
                        }
                        group("build") {
                            prop("mvnBuildProfiles") {
                                default("fedDev")
                                description("Comma delimited")
                                optional()
                            }
                            prop("mvnBuildArgs") {
                                description("Added extra")
                                optional()
                            }
                        }
                        group("deploy") {
                            prop("aemPackageDeployAvoidance") {
                                label("Avoidance")
                                description("Avoids uploading and installing package if identical is already deployed on instance.")
                                checkbox(true)
                            }
                            prop("aemaemPackageDamAssetToggle") {
                                label("Toggle DAM Workflows")
                                description("Turns on/off temporary disablement of assets processing for package deployment time.\n" +
                                        "Useful to avoid redundant rendition generation when package contains renditions synchronized earlier.")
                                checkbox(true)
                            }
                        }
                        group("authorization") {
                            prop("companyUser") {
                                default(System.getProperty("user.name").orEmpty())
                                description("For resolving AEM files from authorized URL")
                                optional()
                            }
                            prop("companyPassword") {
                                optional()
                            }
                            prop("companyDomain") {
                                default(System.getenv("USERDOMAIN").orEmpty())
                                description("For files resolved using SMB")
                                optional()
                            }
                        }
                    }
                }
                
                allprojects {
                    repositories {
                        mavenCentral()
                    }
                }
                
                aem {
                    mvnBuild {
                        $mavenRootDir
                        depGraph {
                            // softRedundantModule("ui.content" to "ui.apps")
                        }
                        discover()
                    }
                }
                
                if (config.captured) {
                    if (aem.mvnBuild.available) defaultTasks(":env:setup")
                    else defaultTasks(":env:instanceSetup")
                }
                """.trimIndent()
            )
        }
    }
}
