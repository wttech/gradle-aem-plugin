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
                implementation("com.neva.gradle:fork-plugin:7.0.11")
                implementation("com.cognifide.gradle:environment-plugin:2.2.3")
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
                        "dispatcher" {
                            load("dispatcherImage") { aem.localInstanceManager.dispatcherImage }
                            resolve { listOf("cache", "logs").forEach { ensureDir(aem.localInstanceManager.dispatcherDir.resolve(it)) } }
                            reload { cleanDir("/mnt/var/www/html") }
                            dev { watchRootDir("app/aem/maven/dispatcher/src") }
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
                    deploy:
                      replicas: 1
                    ports:
                      - 8080:80
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
                      - {{ rootPath }}/dispatcher/src:/mnt/dev/src:ro
                      - {{ rootPath }}/.gradle/aem/localInstance/sdk/dispatcher/lib:/usr/lib/dispatcher-sdk:ro
                      - {{ rootPath }}/.gradle/aem/localInstance/sdk/dispatcher/lib/import_sdk_config.sh:/docker_entrypoint.d/zzz-import-sdk-config.sh:ro
                      # Enable invalidation by any client
                      - {{ rootPath }}/.gradle/aem/localInstance/sdk/dispatcher/lib/overwrite_cache_invalidation.sh:/docker_entrypoint.d/zzz-overwrite_cache_invalidation.sh:ro
                      # Enable hot reload
                      - {{ rootPath }}/.gradle/aem/localInstance/sdk/dispatcher/lib/httpd-reload-monitor:/usr/sbin/httpd-reload-monitor:ro
                      - {{ rootPath }}/.gradle/aem/localInstance/sdk/dispatcher/bin/validator-linux-amd64:/usr/sbin/validator:ro
                      # Enable previewing logs and caches directly on host
                      - {{ rootPath }}/.gradle/aem/localInstance/sdk/dispatcher/logs:/var/log/apache2
                      - {{ rootPath }}/.gradle/aem/localInstance/sdk/dispatcher/cache:/mnt/var/www
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
