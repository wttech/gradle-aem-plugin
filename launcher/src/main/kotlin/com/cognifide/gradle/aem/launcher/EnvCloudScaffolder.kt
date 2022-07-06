package com.cognifide.gradle.aem.launcher

class EnvCloudScaffolder(private val launcher: Launcher) {
    fun scaffold() {
        saveBuildSrc()
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
                implementation("com.cognifide.gradle:aem-plugin:${launcher.pluginVersion}")
                implementation("com.cognifide.gradle:common-plugin:1.0.41")
                implementation("com.neva.gradle:fork-plugin:7.0.5")
                implementation("com.cognifide.gradle:environment-plugin:2.2.0")
            }
            """.trimIndent()
        )
    }

    @Suppress("LongMethod", "MaxLineLength")
    private fun saveEnvBuildScript() = launcher.workFileOnce("env/build.gradle.kts") {
        println("Saving environment Gradle build script file '$this'")
        writeText(
            """
            plugins {
                id("com.cognifide.aem.instance.local")
                id("com.cognifide.environment")
            }
            
            val instancePassword = common.prop.string("instance.default.password")
            val publishHttpUrl = common.prop.string("publish.httpUrl") ?: aem.findInstance("local-publish")?.httpUrl?.orNull ?: "http://127.0.0.1:4503"
            val dispatcherHttpUrl = common.prop.string("dispatcher.httpUrl") ?: "http://127.0.0.1:80"
            val dispatcherTarUrl = common.prop.string("dispatcher.tarUrl") ?: "https://download.macromedia.com/dispatcher/download/dispatcher-apache2.4-linux-x86_64-4.3.4.tar.gz"
            val servicePackUrl = common.prop.string("localInstance.spUrl")
            val coreComponentsUrl = common.prop.string("localInstance.coreComponentsUrl")

            aem {
                instance { // https://github.com/Cognifide/gradle-aem-plugin/blob/master/docs/instance-plugin.md
                    provisioner {
                        enableCrxDe()
                        servicePackUrl?.let { deployPackage(it) }
                        coreComponentsUrl?.let { deployPackage(it) }
                        configureReplicationAgentAuthor("publish") {
                            agent { configure(transportUri = "${'$'}publishHttpUrl/bin/receive?sling:authRequestLogin=1", transportUser = "admin", transportPassword = instancePassword, userId = "admin") }
                            version.set(publishHttpUrl)
                        }
                        configureReplicationAgentPublish("flush") {
                            agent { configure(transportUri = "${'$'}dispatcherHttpUrl/dispatcher/invalidate.cache") }
                            version.set(dispatcherHttpUrl)
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
                                symlink(
                                    "/etc/httpd.extra/conf.modules.d/02-dispatcher.conf" to "/etc/httpd/conf.modules.d/02-dispatcher.conf",
                                    "/etc/httpd.extra/conf.d/variables/default.vars" to "/etc/httpd/conf.d/variables/default.vars"
                                )
                                ensureDir("/usr/local/apache2/logs", "/var/www/localhost/htdocs", "/var/www/localhost/cache")
                                execShell("Starting HTTPD server", "/usr/sbin/httpd -k start")
                            }
                            reload {
                                cleanDir("/var/www/localhost/cache")
                                execShell("Restarting HTTPD server", "/usr/sbin/httpd -k restart")
                            }
                            dev {
                                watchRootDir(
                                    "dispatcher/src/conf.d",
                                    "dispatcher/src/conf.dispatcher.d",
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
                        http("Author Sites Editor", "${'$'}{instance.httpUrl}/sites.html") {
                            containsText("Sites")
                            options { basicCredentials = instance.credentials }
                        }
                        http("Author Replication Agent - Publish", "${'$'}{instance.httpUrl}/etc/replication/agents.author/publish.test.html") {
                            containsText("succeeded")
                            options { basicCredentials = instance.credentials }
                        }
                    }
                    aem.findInstance("local-publish")?.let { instance ->
                        http("Publish Replication Agent - Flush", "${'$'}{instance.httpUrl}/etc/replication/agents.publish/flush.test.html") {
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
                instanceSetup { if (rootProject.aem.mvnBuild.available) dependsOn(":all:deploy") }
                instanceResolve { dependsOn(":requireProps") }
                instanceCreate { dependsOn(":requireProps") }
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
                    deploy:
                      replicas: 1
                    ports:
                      - "80:80"
                    volumes:
                      - "{{ rootPath }}/dispatcher/src/conf.d:/etc/httpd/conf.d"
                      - "{{ rootPath }}/dispatcher/src/conf.dispatcher.d:/etc/httpd/conf.dispatcher.d"
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
