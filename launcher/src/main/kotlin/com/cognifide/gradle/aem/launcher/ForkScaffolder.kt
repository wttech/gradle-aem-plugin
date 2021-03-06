package com.cognifide.gradle.aem.launcher

class ForkScaffolder(private val launcher: Launcher) {

    fun scaffold() {
        savePropertiesTemplate()
        savePropertiesDefinitions()
    }

    private fun savePropertiesTemplate() = launcher.workFileOnce("gradle/fork/gradle.user.properties.peb") {
        println("Saving user-specific properties template '$this'")
        writeText("""
            # === Gradle AEM Plugin ===
            package.manager.deployAvoidance={{packageDeployAvoidance}}
            {% if packageDamAssetToggle == 'true' %}
            package.manager.workflowToggle=[dam_asset=false]
            {% endif %}
            localInstance.quickstart.jarUrl={{ localInstanceQuickstartJarUri }}
            localInstance.quickstart.licenseUrl={{ localInstanceQuickstartLicenseUri }}
            localInstance.openMode={{ localInstanceOpenMode }}
            instance.default.type={{instanceType}}
            instance.default.password={{instancePassword}}
            instance.local-author.enabled={{instanceAuthorEnabled}}
            instance.local-author.httpUrl={{instanceAuthorHttpUrl}}
            instance.local-author.openPath=/aem/start.html
            instance.local-publish.enabled={{instancePublishEnabled}}
            instance.local-publish.httpUrl={{instancePublishHttpUrl}}
            instance.local-publish.openPath=/crx/packmgr

            mvnBuild.args={{mvnBuildArgs}}
            mvnBuild.profiles={{mvnBuildProfiles}}
            
            dispatcher.tarUrl={{ dispatcherTarUri }}

            # === Gradle Environment Plugin ===
            {% if dockerSafeVolumes == 'true' %}
            docker.desktop.safeVolumes=true
            {% endif %}

            # === Gradle Common Plugin ===
            notifier.enabled=true
            fileTransfer.user={{companyUser}}
            fileTransfer.password={{companyPassword}}
            fileTransfer.domain={{companyDomain}}
        """.trimIndent())
    }

    private fun savePropertiesDefinitions() = launcher.workFileOnce("gradle/fork/props.gradle.kts") {
        println("Saving user-specific property definitions '$this'")
        writeText("""
            import com.cognifide.gradle.aem.common.instance.local.OpenMode
            import com.neva.gradle.fork.ForkExtension

            configure<ForkExtension> {
                properties {
                    group("Instances") {
                        define("instanceType") {
                            label = "Type"
                            select("local", "remote")
                            description = "Local - instance will be created on local file system\nRemote - connecting to remote instance only"
                            controller { toggle(value == "local", "instanceRunModes", "instanceJvmOpts", "localInstance*") }
                        }
                        define("instanceAuthorHttpUrl") {
                            label = "Author HTTP URL"
                            url("http://localhost:4502")
                            optional()
                        }
                        define("instanceAuthorEnabled") {
                            label = "Author Enabled"
                            checkbox(true)
                        }
                        define("instancePublishHttpUrl") {
                            label = "Publish HTTP URL"
                            url("http://localhost:4503")
                            optional()
                        }
                        define("instancePublishEnabled") {
                            label = "Publish Enabled"
                            checkbox(true)
                        }
                        define("instancePassword") {
                            label = "Password"
                            password("admin")
                            optional()
                        }
                        define("localInstanceQuickstartJarUri") {
                            label = "Quickstart URI"
                            description = "Typically file named 'cq-quickstart-*.jar' or 'aem-sdk-quickstart-*.jar"
                        }
                        define("localInstanceQuickstartLicenseUri") {
                            label = "Quickstart License URI"
                            description = "Typically file named 'license.properties'"
                        }
                        define("localInstanceOpenMode") {
                            label = "Open Automatically"
                            description = "Open web browser when instances are up."
                            select(OpenMode.values().map { it.name.toLowerCase() }, OpenMode.ALWAYS.name.toLowerCase())
                        }
                    }
                    group("Dispatcher") {
                        define("dispatcherTarUri") {
                            label = "Tar Archive URI"
                            description = "Typically file named 'dispatcher-apache2.4-linux-x86_64-*.tar.gz'"
                            text("http://download.macromedia.com/dispatcher/download/dispatcher-apache2.4-linux-x86_64-4.3.3.tar.gz")
                        }
                        define("dockerSafeVolumes") {
                            label = "Docker Safe Volumes"
                            description = "Enables volumes for easily previewing e.g cache and logs (requires WSL2)"
                            checkbox(false)
                            dynamic("props")
                        }
                    }
                    group("Build") {
                        define("mvnBuildProfiles") {
                            label = "Maven Profiles"
                            text("fedDev")
                            description = "Comma delimited"
                            optional()
                        }
                        define("mvnBuildArgs") {
                            label = "Maven Args"
                            description = "Added extra"
                            optional()
                        }
                        define("packageDeployAvoidance") {
                            label = "Deploy Avoidance"
                            description = "Avoids uploading and installing package if identical is already deployed on instance."
                            checkbox(true)
                        }
                        define("packageDamAssetToggle") {
                            label = "Deploy Without DAM Worklows"
                            description = "Turns on/off temporary disablement of assets processing for package deployment time.\n" +
                                    "Useful to avoid redundant rendition generation when package contains renditions synchronized earlier."
                            checkbox(true)
                            dynamic("props")
                        }
                    }
                    group("Deploy") {
                        define("packageDeployAvoidance") {
                            label = "Avoidance"
                            description = "Avoids uploading and installing package if identical is already deployed on instance."
                            checkbox(true)
                        }
                        define("packageDamAssetToggle") {
                            label = "Toggle DAM Worklows"
                            description = "Turns on/off temporary disablement of assets processing for package deployment time.\n" +
                                    "Useful to avoid redundant rendition generation when package contains renditions synchronized earlier."
                            checkbox(true)
                            dynamic("props")
                        }
                    }
                    group("Authorization") {
                        define("companyUser") {
                            label = "User"
                            text(System.getProperty("user.name").orEmpty())
                            description = "For resolving AEM files from authorized URL"
                            optional()
                        }
                        define("companyPassword") {
                            label = "Password"
                            optional()
                        }
                        define("companyDomain") {
                            label = "Domain"
                            text(System.getenv("USERDOMAIN").orEmpty())
                            description = "For files resolved using SMB"
                            optional()
                        }
                    }
                }
            }
        """.trimIndent())
    }
}