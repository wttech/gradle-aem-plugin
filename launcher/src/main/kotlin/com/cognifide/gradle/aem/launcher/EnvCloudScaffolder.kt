package com.cognifide.gradle.aem.launcher

class EnvCloudScaffolder(private val launcher: Launcher) {
    fun scaffold() {
        saveBuildSrc()
        saveRootBuildScript()
        savePropertiesTemplate()
        saveEnvBuildScript()
        saveEnvSrcFiles()
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
                implementation("com.cognifide.gradle:common-plugin:1.1.15")
                implementation("com.cognifide.gradle:environment-plugin:2.2.5")
            }
            """.trimIndent()
        )
    }

    @Suppress("LongMethod", "MaxLineLength")
    private fun saveRootBuildScript() = launcher.workFileOnce("build.gradle.kts") {
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
                            description("Typically file named 'aem-sdk-yyyy.mm.nnnn*.zip'")
                        }
                        prop("aemQuickstartLicenseUrl") {
                            description("Typically file named 'license.properties'")
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
            
            localInstance.quickstart.distUrl={{ config.aemQuickstartDistUrl }}
            localInstance.quickstart.licenseUrl={{ config.aemQuickstartLicenseUrl }}
            localInstance.openMode={{ config.aemInstanceOpenMode }}
            
            instance.default.runModes={{ config.aemInstanceRunModes }}
            instance.default.password={{ config.aemInstancePassword }}
            
            instance.{{ config.aemInstanceType }}-author.serviceCredentialsUrl={{ config.aemInstanceServiceCredentialsUrl }}
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

    @Suppress("LongMethod", "MaxLineLength")
    private fun saveEnvBuildScript() = launcher.workFileOnce("env/build.gradle.kts") {
        println("Saving environment Gradle build script file '$this'")
        writeText(
            """
            import io.wttech.gradle.config.dsl.*    
                
            plugins {
                id("com.cognifide.aem.instance.local")
                id("com.cognifide.environment")
            }
            
            val instancePassword = config.stringValue("aemInstancePassword")
            val publishHttpUrl = config.stringValue("aemPublishHttpUrl")
            val flushHttpUrl = config.stringValue("aemFlushHttpUrl")
            
            aem {
                instance { // https://github.com/Cognifide/gradle-aem-plugin/blob/main/docs/instance-plugin.md
                    provisioner {
                        enableCrxDe()
                        configureReplicationAgentAuthor("publish") {
                           agent(mapOf(
                                "enabled" to true,
                                "transportUri" to "${'$'}publishHttpUrl/bin/receive?sling:authRequestLogin=1",
                                "transportUser" to "admin",
                                "transportPassword" to instancePassword,
                                "userId" to "admin"
                            ))
                        }
                        configureReplicationAgentPublish("flush") {
                           agent(mapOf(
                                "enabled" to true,
                                "transportUri" to "${'$'}flushHttpUrl/dispatcher/invalidate.cache",
                                "protocolHTTPHeaders" to listOf("CQ-Action:{action}", "CQ-Handle:{path}", "CQ-Path: {path}", "Host: publish")
                            ))
                        }
                    }
                }
            }
            
            environment { // https://github.com/Cognifide/gradle-environment-plugin
                docker {
                    containers {
                        "dispatcher" {
                            load("dispatcherImage") { aem.localInstanceManager.dispatcherImage }
                            resolve { listOf("cache", "logs").forEach { ensureDir(aem.localInstanceManager.dispatcherDir.resolve(it)) } }
                            reload { cleanDir("/mnt/var/www/html") }
                            dev { watchRootDir("${launcher.appDirPath}/dispatcher/src") }
                        }
                    }
                }
                hosts {
                    "http://publish.aem.local" { tag("publish") }
                }
                healthChecks {
                    aem.findInstance("local-author")?.let { instance ->
                        http("Author Sites Editor", "${'$'}{instance.httpUrl.get()}/sites.html") {
                            containsText("Sites")
                            options { basicCredentials = instance.credentials }
                        }
                        http("Author Replication Agent - Publish", "${'$'}{instance.httpUrl.get()}/etc/replication/agents.author/publish.test.html") {
                            containsText("succeeded")
                            options { basicCredentials = instance.credentials }
                        }
                    }
                    aem.findInstance("local-publish")?.let { instance ->
                        http("Publish Replication Agent - Flush", "${'$'}{instance.httpUrl.get()}/etc/replication/agents.publish/flush.test.html") {
                            containsText("succeeded")
                            options { basicCredentials = instance.credentials }
                        }
                        /*
                        http("Site Home", "http://publish.aem.local/us/en.html") {
                            containsText("My Site")
                        }
                        */
                    }
                }
            }

            tasks {
                instanceSetup { if (rootProject.aem.mvnBuild.available) dependsOn(":all:packageDeploy") }
                instanceResolve { requiresConfig() }
                instanceCreate { requiresConfig() }
                environmentUp { mustRunAfter(instanceAwait, instanceUp, instanceProvision, instanceSetup) }
                environmentAwait { mustRunAfter(instanceAwait, instanceUp, instanceProvision, instanceSetup) }
            }
            """.trimIndent()
        )
    }

    @Suppress("LongMethod", "MaxLineLength")
    private fun saveEnvSrcFiles() {
        launcher.workFileOnce("env/src/environment/docker-compose.yml.peb") {
            println("Saving environment Docker compose file '$this'")
            writeText(
                """
                version: "3"
                services:
                  dispatcher:
                    image: {{ dispatcherImage }}
                    ports:
                      - 80:80
                    environment:
                      - AEM_HOST=host.docker.internal
                      - AEM_IP=*.*.*.*
                      - AEM_PORT=4503
                      - VHOST=publish
                      - ENVIRONMENT_TYPE=dev
                      - DISP_LOG_LEVEL=Warn
                      - REWRITE_LOG_LEVEL=Warn
                      - EXPIRATION_TIME=A2592000
                      - FORWARDED_HOST_SETTING=Off
                      - COMMERCE_ENDPOINT=http://localhost/graphql  
                    volumes:
                      # Use project-specific dispatcher config
                      - {{ rootPath }}/${launcher.appDirPath}/dispatcher/src:/mnt/dev/src:ro
                      - {{ rootPath }}/env/.gradle/aem/localInstance/sdk/dispatcher/lib:/usr/lib/dispatcher-sdk:ro
                      - {{ rootPath }}/env/.gradle/aem/localInstance/sdk/dispatcher/lib/import_sdk_config.sh:/docker_entrypoint.d/zzz-import-sdk-config.sh:ro
                      # Enable invalidation by any client
                      - {{ rootPath }}/env/.gradle/aem/localInstance/sdk/dispatcher/lib/overwrite_cache_invalidation.sh:/docker_entrypoint.d/zzz-overwrite_cache_invalidation.sh:ro
                      # Enable hot reload
                      - {{ rootPath }}/env/.gradle/aem/localInstance/sdk/dispatcher/lib/httpd-reload-monitor:/usr/sbin/httpd-reload-monitor:ro
                      - {{ rootPath }}/env/.gradle/aem/localInstance/sdk/dispatcher/bin/validator-linux-amd64:/usr/sbin/validator:ro
                      # Enable previewing logs and caches directly on host
                      - {{ rootPath }}/env/.gradle/aem/localInstance/sdk/dispatcher/logs:/var/log/apache2
                      - {{ rootPath }}/env/.gradle/aem/localInstance/sdk/dispatcher/cache:/mnt/var/www
                    {% if docker.runtime.hostInternalIpMissing %}
                    extra_hosts:
                      - "host.docker.internal:{{ docker.runtime.hostInternalIp }}"
                    {% endif %}
                """.trimIndent()
            )
        }

        launcher.workFileOnce("env/src/environment/httpd/conf.modules.d/02-dispatcher.conf") {
            println("Saving environment HTTPD dispatcher config file '$this'")
            writeText(
                """
                LoadModule dispatcher_module modules/mod_dispatcher.so
                """.trimIndent()
            )
        }

        launcher.workFileOnce("env/src/environment/httpd/conf.d/variables/default.vars") {
            println("Saving environment variables file '$this'")
            writeText(
                """
                Define DOCROOT /var/www/localhost/cache
                Define AEM_HOST host.docker.internal
                Define AEM_IP *.*.*.*
                Define AEM_PORT 4503
                Define DISP_LOG_LEVEL Warn
                Define REWRITE_LOG_LEVEL Warn
                Define EXPIRATION_TIME A2592000
                Define FORWARDED_HOST_SETTING Off
                Define COMMERCE_ENDPOINT https://publish.aem.local/api/graphql
                """.trimIndent()
            )
        }
    }
}
