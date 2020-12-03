package com.cognifide.gradle.aem.bundle

import com.cognifide.gradle.aem.test.AemBuildTest
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test

@Suppress("LongMethod", "MaxLineLength")
class BundlePluginTest : AemBuildTest() {

    @Test
    fun `should build bundle using minimal configuration`() {
        val projectDir = prepareProject("bundle-minimal") {
            settingsGradle("")

            buildGradle("""
                plugins {
                    id("com.cognifide.aem.bundle")
                }
                
                group = "com.company.example"
                
                dependencies {
                    compileOnly("org.slf4j:slf4j-api:1.5.10")
                    compileOnly("org.osgi:osgi.cmpn:6.0.0")
                }
                """)

            helloServiceJava()
        }

        runBuild(projectDir, "jar", "-Poffline") {
            assertTask(":jar")
            assertBundle("build/libs/bundle-minimal.jar")
            assertZipEntryEquals("build/libs/bundle-minimal.jar", "OSGI-INF/com.company.example.aem.HelloService.xml", """
                <?xml version="1.0" encoding="UTF-8"?>
                <scr:component xmlns:scr="http://www.osgi.org/xmlns/scr/v1.3.0" name="com.company.example.aem.HelloService" immediate="true" activate="activate" deactivate="deactivate">
                  <service>
                    <provide interface="com.company.example.aem.HelloService"/>
                  </service>
                  <implementation class="com.company.example.aem.HelloService"/>
                </scr:component>
            """)
        }
    }

    @Test
    fun `should build bundle using extended configuration`() {
        val projectDir = prepareProject("bundle-extended") {
            settingsGradle("")

            buildGradle("""
                import com.cognifide.gradle.aem.bundle.tasks.bundle
                
                plugins {
                    id("com.cognifide.aem.bundle")
                    id("maven-publish")
                }
                
                group = "com.company.example"
                version = "1.0.0"
                
                repositories {
                    maven("https://repo.adobe.com/nexus/content/groups/public")
                }
                
                dependencies {
                    compileOnly("org.slf4j:slf4j-api:1.5.10")
                    compileOnly("org.osgi:osgi.cmpn:6.0.0")
                    compileOnly("com.adobe.aem:uber-jar:6.5.0:apis")
                    
                    testImplementation("org.junit.jupiter:junit-jupiter:5.5.2")
                    // testImplementation("io.wcm:io.wcm.testing.aem-mock.junit5:2.5.2")
                }

                tasks {
                    jar {
                        bundle {
                            category = "example"
                            vendor = "Company"
                        }
                    }
                }
                
                publishing {
                    repositories {
                        maven(rootProject.file("build/repository"))
                    }
                
                    publications {
                        create<MavenPublication>("maven") {
                            from(components["java"])
                        }
                    }
                }
                """)

            helloServiceJava()
        }

        runBuild(projectDir, "jar", "-Poffline") {
            assertTask(":jar")
            assertBundle("build/libs/bundle-extended-1.0.0.jar")
        }

        runBuild(projectDir, "publish", "-Poffline") {
            assertTask(":jar", TaskOutcome.UP_TO_DATE)

            val mavenDir = projectDir.resolve("build/repository/com/company/example/bundle-extended/1.0.0")
            assertFileExists(mavenDir.resolve("bundle-extended-1.0.0.jar"))
            assertFileExists(mavenDir.resolve("bundle-extended-1.0.0.pom"))
            assertFileExists(mavenDir.resolve("bundle-extended-1.0.0.module"))
        }
    }

    @Test
    fun `should build bundle with embed code`() {
        val projectDir = prepareProject("bundle-embed") {
            settingsGradle("")

            buildGradle("""
                import com.cognifide.gradle.aem.bundle.tasks.bundle
                
                plugins {
                    id("com.cognifide.aem.bundle")
                }
                
                group = "com.company.example"
                
                dependencies {
                    compileOnly("org.slf4j:slf4j-api:1.5.10")
                    compileOnly("org.osgi:osgi.cmpn:6.0.0")
                }
                
                tasks {
                    jar {
                        bundle {
                            embedPackage("org.hashids:hashids:1.0.1", "org.hashids")
                        }
                    }
                }
                """)

            helloServiceJava()
        }

        runBuild(projectDir, "jar", "-Poffline") {
            assertTask(":jar")

            val jar = file("build/libs/bundle-embed.jar")

            assertBundle(jar)

            assertZipEntry(jar, "OSGI-INF/com.company.example.aem.HelloService.xml")
            assertZipEntry(jar, "com/company/example/aem/HelloService.class")
            assertZipEntry(jar, "org/hashids/Hashids.class")
        }
    }
}
