package com.cognifide.gradle.aem.launcher

@Suppress("LongMethod", "MaxLineLength")
class EnvOnPremScaffolder(private val launcher: Launcher) {
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
                implementation("com.cognifide.gradle:common-plugin:1.0.41")
                implementation("com.cognifide.gradle:environment-plugin:2.2.0")
            }
            """.trimIndent()
        )
    }

    @Suppress("LongMethod")
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
            val dispatcherTarUrl = config.stringValue("aemDispatcherTarUrl") // "https://download.macromedia.com/dispatcher/download/dispatcher-apache2.4-linux-x86_64-4.3.5.tar.gz"
            val flushHttpUrl = config.stringValue("aemFlushHttpUrl")
            
            val servicePackUrl = config.stringValue("aemServicePackUrl").ifBlank { null }
            val coreComponentsUrl = config.stringValue("aemCoreComponentsUrl").ifBlank { null }

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
                        "httpd" {
                            resolve {
                                resolveFiles {
                                    download(dispatcherTarUrl).use {
                                        copyArchiveFile(it, "**/dispatcher-apache*.so", workFile("modules/mod_dispatcher.so"))
                                    }
                                }
                                ensureDir("htdocs", "cache", "logs")
                            }
                            up {
                                ensureDir(
                                    "/usr/local/apache2/logs",
                                    "/var/www/localhost/htdocs",
                                    "/var/www/localhost/author/cache",
                                    "/var/www/localhost/publish/cache",
                                    "/etc/httpd/conf.d/proxy",
                                    "/mnt/var/www/default"
                                )
                                symlink(
                                    "/etc/httpd.extra/conf.modules.d/02-dispatcher.conf" to "/etc/httpd/conf.modules.d/02-dispatcher.conf",
                                    "/etc/httpd.extra/conf.d/proxy/mock.proxy" to "/etc/httpd/conf.d/proxy/mock.proxy",
                                    "/etc/httpd.extra/conf.d/variables/default.vars" to "/etc/httpd/conf.d/variables/default.vars",
                                    "/etc/httpd.extra/conf.d/custom.conf" to "/etc/httpd/conf.d/custom.conf"
                                )
                                execShell("Installing SSL module", "yum -y install mod_ssl openssh")
                                execShell("Starting HTTPD server", "/usr/sbin/httpd -k start")
                            }
                            reload {
                                cleanDir("/var/www/localhost/cache")
                                execShell("Restarting HTTPD server", "/usr/sbin/httpd -k restart")
                            }
                            dev {
                                watchRootDir(
                                    "${launcher.appDirPath}/dispatcher/src/conf.d",
                                    "${launcher.appDirPath}/dispatcher/src/conf.dispatcher.d",
                                    "env/src/environment/httpd")
                            }
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

    private fun saveEnvSrcFiles() {
        launcher.workFileOnce("env/src/environment/docker-compose.yml.peb") {
            println("Saving environment Docker compose file '$this'")
            writeText(
                """
                version: "3"
                services:
                  httpd:
                    image: centos/httpd:latest
                    command: ["tail", "-f", "--retry", "/usr/local/apache2/logs/error.log"]
                    ports:
                      - 80:80
                    volumes:
                      - "{{ rootPath }}/${launcher.appDirPath}/dispatcher/src/conf.d:/etc/httpd/conf.d"
                      - "{{ rootPath }}/${launcher.appDirPath}/dispatcher/src/conf.dispatcher.d:/etc/httpd/conf.dispatcher.d"
                      - "{{ sourcePath }}/httpd:/etc/httpd.extra"
                      - "{{ workPath }}/httpd/modules/mod_dispatcher.so:/etc/httpd/modules/mod_dispatcher.so"
                      - "{{ workPath }}/httpd/logs:/etc/httpd/logs"
                      {% if docker.runtime.safeVolumes %}
                      - "{{ workPath }}/httpd/cache:/var/www/localhost/cache"
                      - "{{ workPath }}/httpd/htdocs:/var/www/localhost/htdocs"
                      {% endif %}
                    {% if docker.runtime.hostInternalIpMissing %}
                    extra_hosts:
                      - "host.docker.internal:{{ docker.runtime.hostInternalIp }}"
                    {% endif %}
                    environment:
                      - DISP_ID=docker
                """.trimIndent()
            )
        }

        launcher.workFileOnce("env/src/environment/httpd/conf.d/variables/default.vars") {
            println("Saving environment variables file '$this'")
            writeText(
                """
                Define DISP_LOG_LEVEL Warn
                Define REWRITE_LOG_LEVEL Warn
                Define EXPIRATION_TIME A2592000
                Define CRX_FILTER deny
                Define FORWARDED_HOST_SETTING Off
                Define AUTHOR_DOCROOT /var/www/localhost/author/cache
                Define AUTHOR_DEFAULT_HOSTNAME author.aem.local
                Define AUTHOR_IP host.docker.internal
                Define AUTHOR_PORT 4502
                Define PUBLISH_DOCROOT /var/www/localhost/publish/cache
                Define PUBLISH_DEFAULT_HOSTNAME publish.aem.local
                Define PUBLISH_IP host.docker.internal
                Define PUBLISH_PORT 4503
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

        launcher.workFileOnce("env/src/environment/httpd/conf.d/custom.conf") {
            println("Saving environment HTTPD dispatcher config file '$this'")
            writeText(
                """
                Include conf.d/variables/default.vars
                """.trimIndent()
            )
        }

        launcher.workFileOnce("env/src/environment/httpd/conf.d/proxy/mock.proxy") {
            println("Creating mock proxy configuration file '$this'")
            writeText("".trimIndent())
        }

        launcher.workFileBackupOnce("${launcher.appDirPath}/dispatcher/src/conf.dispatcher.d/cache/ams_author_invalidate_allowed.any") {
            println("Creating author flush config file '$this'")
            writeText(
                """
                # This is where you'd put an entry for each publisher or author that you want to allow to invalidate the cache on the dispatcher
                /0 {
                    /glob "*.*.*.*"
                    /type "allow"
                }
                """.trimIndent()
            )
        }

        launcher.workFileBackupOnce("${launcher.appDirPath}/dispatcher/src/conf.dispatcher.d/cache/ams_publish_invalidate_allowed.any") {
            println("Creating publish flush config file '$this'")
            writeText(
                """
                # This is where you'd put an entry for each publisher or author that you want to allow to invalidate the cache on the dispatcher
                /0 {
                    /glob "*.*.*.*"
                    /type "allow"
                }
                """.trimIndent()
            )
        }

        launcher.workFileBackupOnce("${launcher.appDirPath}/dispatcher/src/conf.d/rewrites/xforwarded_forcessl_rewrite.rules") {
            println("Creating X-Forwarded-For SSL rewrite config file '$this'")
            writeText(
                """
                # This ruleset forces https in the end users browser
                #RewriteCond %{HTTP:X-Forwarded-Proto} !https
                #RewriteCond %{REQUEST_URI} !^/dispatcher/invalidate.cache
                #RewriteRule (.*) https://%{SERVER_NAME}%{REQUEST_URI} [L,R=301]
                """.trimIndent()
            )
        }

        /**
         * Replacing md5 checksums in dispatcher's pom.xml for the files that are being replaced
         */
        launcher.workFileBackupAndReplaceStrings("${launcher.appDirPath}/dispatcher/pom.xml",
            // Replacing md5 checksum for xforwarded_forcessl_rewrite.rules file
            "cd1373a055f245de6e9ed78f74f974a6" to "3f6158d0fd659071fa29c50c9a509804",
            // Replacing md5 checksum for ams_author_invalidate_allowed.any and ams_publish_invalidate_allowed.any files
            "a66be278d68472073241fc78db7af993" to "122cecacb81e64d1c1c47f68d082bef1"
        )
    }
}
