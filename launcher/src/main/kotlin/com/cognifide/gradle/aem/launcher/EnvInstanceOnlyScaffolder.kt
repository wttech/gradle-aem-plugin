package com.cognifide.gradle.aem.launcher

class EnvInstanceOnlyScaffolder(private val launcher: Launcher) {
    fun scaffold() {
        saveBuildSrc()
        saveRootBuildScript()
        savePropertiesTemplate()
        saveEnvBuildScript()
    }
    private fun saveBuildSrc() = launcher.workFileOnce("buildSrc/build.gradle.kts") {
        println("Saving Gradle build source script file '$this'")
        writeText(
            """
            repositories {
                mavenLocal()
                mavenCentral()
                gradlePluginPortal()
            }
            
            dependencies {
                implementation("io.wttech.gradle.config:plugin:1.0.10")
                implementation("com.cognifide.gradle:aem-plugin:${launcher.pluginVersion}")
                implementation("com.cognifide.gradle:common-plugin:1.0.41")
            }
            """.trimIndent()
        )
    }

    @Suppress("LongMethod", "MaxLineLength")
    private fun saveRootBuildScript() {
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
                                default(OpenMode.ALWAYS.name.toLowerCase())
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

    private fun savePropertiesTemplate() = launcher.workFileOnce("gradle.properties.peb") {
        println("Saving user-specific properties template '$this'")
        writeText(
            """
            # === Gradle    
            
            org.gradle.logging.level=info
            org.gradle.daemon=true
            org.gradle.parallel=true
            org.gradle.caching=true
            org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8 
                
            # === Gradle AEM Plugin ===
            
            package.manager.deployAvoidance={{ config.aemPackageDeployAvoidance }}
            {% if aemPackageDamAssetToggle == 'true' %}
            package.manager.workflowToggle=[dam_asset=false]
            {% endif %}
            
            localInstance.quickstart.distUrl={{ config.aemQuickstartDistUri }}
            localInstance.quickstart.licenseUrl={{ config.aemQuickstartLicenseUri }}
            localInstance.openMode={{ config.aemInstanceOpenMode }}
            
            instance.default.runModes={{ config.aemInstanceRunModes }}
            instance.default.password={{ config.aemInstancePassword }}
            
            instance.{{ config.aemInstanceType }}-author.serviceCredentialsUrl={{ config.aemInstanceServiceCredentialsUri }}
            instance.{{ config.aemInstanceType }}-author.enabled={{ config.aemAuthorEnabled }}
            instance.{{ config.aemInstanceType }}-author.httpUrl={{ config.aemAuthorHttpUrl }}
            instance.{{ config.aemInstanceType }}-author.openPath=/aem/start.html
            instance.{{ config.aemInstanceType }}-publish.enabled={{ config.aemPublishEnabled }}
            instance.{{ config.aemInstanceType }}-publish.httpUrl={{ config.aemPublishHttpUrl }}
            instance.{{ config.aemInstanceType }}-publish.openPath=/crx/packmgr

            mvnBuild.args={{ config.mvnBuildArgs }}

            # === Gradle Common Plugin ===
            
            javaSupport.version=11
            
            notifier.enabled=true
            
            fileTransfer.user={{ config.companyUser }}
            fileTransfer.password={{ config.companyPassword }}
            fileTransfer.domain={{ config.companyDomain }}
            """.trimIndent()
        )
    }

    private fun saveEnvBuildScript() = launcher.workFileOnce("env/build.gradle.kts") {
        println("Saving environment Gradle build script file '$this'")
        writeText(
            """
            plugins {
                id("com.cognifide.aem.instance.local")
                id("com.cognifide.aem.common")
            }
            
            val instancePassword = config.stringValue("aemInstancePassword")
            val publishHttpUrl = config.stringValue("aemPublishHttpUrl")
            
            val servicePackUrl = config.stringValueOrNull("aemServicePackUrl")?.ifBlank { null }
            val coreComponentsUrl = config.stringValueOrNull("aemCoreComponentsUrl")?.ifBlank { null }

            aem {
                instance { // https://github.com/Cognifide/gradle-aem-plugin/blob/main/docs/instance-plugin.md
                    provisioner {
                        enableCrxDe()
                        servicePackUrl?.let { deployPackage(it) }
                        coreComponentsUrl?.let { deployPackage(it) }
                        configureReplicationAgentAuthor("publish") {
                           agent(mapOf(
                                "enabled" to true,
                                "transportUri" to "${'$'}publishHttpUrl/bin/receive?sling:authRequestLogin=1",
                                "transportUser" to "admin",
                                "transportPassword" to instancePassword,
                                "userId" to "admin"
                            ))
                        }
                    }
                }
            }
            """.trimIndent()
        )
    }
}
