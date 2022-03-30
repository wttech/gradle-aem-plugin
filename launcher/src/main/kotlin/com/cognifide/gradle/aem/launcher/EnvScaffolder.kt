package com.cognifide.gradle.aem.launcher

class EnvScaffolder(private val launcher: Launcher) {
    fun scaffold() {
        saveBuildSrc()
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
                implementation("com.cognifide.gradle:aem-plugin:${launcher.pluginVersion}")
                implementation("com.cognifide.gradle:common-plugin:1.0.41")
                implementation("com.neva.gradle:fork-plugin:7.0.5")
            }
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
            val publishHttpUrl = common.prop.string("publish.httpUrl") ?: aem.findInstance("local-publish")?.httpUrl ?: "http://127.0.0.1:4503"
            val servicePackUrl = common.prop.string("localInstance.spUrl")
            val coreComponentsUrl = common.prop.string("localInstance.coreComponentsUrl")

            aem {
                instance { // https://github.com/Cognifide/gradle-aem-plugin/blob/master/docs/instance-plugin.md
                    provisioner {
                        enableCrxDe()
                        servicePackUrl?.let { deployPackage(it) }
                        coreComponentsUrl?.let { deployPackage(it) }
                        configureReplicationAgentAuthor("publish") {
                            agent { configure(
                                transportUri = "${'$'}publishHttpUrl/bin/receive?sling:authRequestLogin=1",
                                transportUser = "admin",
                                transportPassword = instancePassword,
                                userId = "admin") }
                            version.set(publishHttpUrl)
                        }
                    }
                }
            }
            """.trimIndent()
        )
    }
}
