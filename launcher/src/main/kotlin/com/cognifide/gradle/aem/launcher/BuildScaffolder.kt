package com.cognifide.gradle.aem.launcher

import java.io.File
import java.util.Properties

class BuildScaffolder(private val launcher: Launcher) {

    fun scaffold() {
        saveProperties()
        saveSettings()
        saveRootBuildScript()
        savePropertiesTemplate()
        when (aemVersion) {
            "cloud" -> EnvCloudScaffolder(launcher).scaffold()
            null -> EnvScaffolder(launcher).scaffold()
            else -> EnvOnPremScaffolder(launcher).scaffold()
        }
    }

    private val aemVersion get() = archetypeProperties?.getProperty("aemVersion")

    private val archetypeProperties get() = if (archetypePropertiesFile.exists()) Properties().apply {
        archetypePropertiesFile.inputStream().buffered().use { load(it) }
    } else null

    private val archetypePropertiesFile get() = File("archetype.properties")

    private fun saveProperties() = launcher.workFileOnce("gradle.properties") {
        println("Saving Gradle properties file '$this'")
        outputStream().use { output ->
            Properties().apply {
                if (savePropsFlag) {
                    putAll(saveProps)
                }
                store(output, null)
            }
        }
    }

    private val savePropsFlag get() = launcher.args.contains(Launcher.ARG_SAVE_PROPS)

    private val saveProps get() = launcher.args.filter { it.startsWith(Launcher.ARG_SAVE_PREFIX) }
        .map { it.removePrefix(Launcher.ARG_SAVE_PREFIX) }
        .associate { it.substringBefore("=") to it.substringAfter("=") }

    @Suppress("LongMethod")
    private fun saveRootBuildScript() = launcher.workFileOnce("build.gradle.kts") {
        println("Saving root Gradle build script file '$this'")

        val mavenRootDir = when {
            launcher.appDirPath.isNullOrBlank() -> ""
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
                    labelAbbrs("aem")
                    valueSaveGradleProperties()
                
                    group("instance") {
                        prop("instanceType") {
                            options("local", "remote")
                            description("Local - instance will be created on local file system\nRemote - connecting to remote instance only")
                        }
                        prop("instanceAuthorHttpUrl") {
                            default("http://localhost:4502")
                            visible { boolValue("instanceAuthorEnabled") }
                            optional()
                        }
                        prop("instanceAuthorEnabled") {
                            checkbox(true)
                        }
                        prop("instancePublishHttpUrl") {
                            default("http://localhost:4503")
                            visible { boolValue("instancePublishEnabled") }
                            optional()
                        }
                        prop("instancePublishEnabled") {
                            checkbox(true)
                        }
                        prop("instancePassword") {
                            default("admin")
                            optional()
                        }
                        prop("instanceServiceCredentialsUri") {
                            label("Service Credentials Uri")
                            description("JSON file downloaded from AEMaaCS developer console")
                            optional()
                        }
                        prop("localInstanceRunModes") {
                            label("Run Modes")
                            optional()
                        }
                        prop("localInstanceQuickstartDistUri") {
                            label("AEM distribution URI")
                            description("Typically AEM SDK zip file or AEM jar file")
                        }
                        prop("localInstanceQuickstartLicenseUri") {
                            label("Quickstart License URI")
                            description("Typically file named 'license.properties'")
                        }
                        prop("localInstanceSpUri") {
                            description("Only for on-prem AEM instances. Typically file named 'aem-service-pkg-*.zip'")
                            optional()
                        }
                        prop("localInstanceCoreComponentsUri") {
                            description("Only for on-prem AEM instances. Typically file named 'core.wcm.components.all-*.zip'")
                            optional()
                        }
                        prop("localInstanceOpenMode") {
                            label("Open Automatically")
                            description("Open web browser when instances are up.")
                            options(OpenMode.values().map { it.name.toLowerCase() })
                            default(OpenMode.ALWAYS.name.toLowerCase())
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
                        prop("packageDeployAvoidance") {
                            label("Avoidance")
                            description("Avoids uploading and installing package if identical is already deployed on instance.")
                            checkbox(true)
                        }
                        prop("packageDamAssetToggle") {
                            label("Toggle DAM Worklows")
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
            
            if (config.captured) {
                if (aem.mvnBuild.available) defaultTasks(":env:setup")
                else defaultTasks(":env:instanceSetup")
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
            """.trimIndent()
        )
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
            
            package.manager.deployAvoidance={{ config.packageDeployAvoidance }}
            {% if packageDamAssetToggle == 'true' %}
            package.manager.workflowToggle=[dam_asset=false]
            {% endif %}
            
            localInstance.quickstart.distUrl={{ config.localInstanceQuickstartDistUri }}
            localInstance.quickstart.licenseUrl={{ config.localInstanceQuickstartLicenseUri }}
            localInstance.openMode={{ config.localInstanceOpenMode }}
            
            localInstance.spUrl={{ config.localInstanceSpUri }}
            localInstance.coreComponentsUrl={{ config.localInstanceCoreComponentsUri }}
            
            instance.default.runModes={{ config.localInstanceRunModes }}
            instance.default.password={{ config.instancePassword }}
            
            instance.{{ config.instanceType }}-author.serviceCredentialsUrl={{ config.instanceServiceCredentialsUri }}
            instance.{{ config.instanceType }}-author.enabled={{ config.instanceAuthorEnabled }}
            instance.{{ config.instanceType }}-author.httpUrl={{ config.instanceAuthorHttpUrl }}
            instance.{{ config.instanceType }}-author.openPath=/aem/start.html
            instance.{{ config.instanceType }}-publish.enabled={{ config.instancePublishEnabled }}
            instance.{{ config.instanceType }}-publish.httpUrl={{ config.instancePublishHttpUrl }}
            instance.{{ config.instanceType }}-publish.openPath=/crx/packmgr

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

    private fun saveSettings() = launcher.workFileOnce("settings.gradle.kts") {
        println("Saving Gradle settings file '$this'")
        writeText(
            """
            include(":env")
            """.trimIndent()
        )
    }
}
