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
    private fun saveRootBuildScript() = EnvCommonScaffolder.saveRootBuildScript(launcher)

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
            
            val instancePassword = common.prop.string("instance.default.password")
            val publishHttpUrl = common.prop.string("publish.httpUrl") ?: aem.findInstance("local-publish")?.httpUrl?.orNull ?: "http://127.0.0.1:4503"
            val servicePackUrl = common.prop.string("localInstance.spUrl")
            val coreComponentsUrl = common.prop.string("localInstance.coreComponentsUrl")

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
