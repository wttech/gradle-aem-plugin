package com.cognifide.gradle.aem.bundle

import com.cognifide.gradle.aem.test.AemBuildTest
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import java.io.File

@Suppress("LongMethod", "MaxLineLength")
class BundlePluginTest : AemBuildTest() {

    @Test
    fun `should build package with bundle using minimal configuration`() {
        val projectDir = prepareProject("bundle-minimal") {
            settingsGradle("")

            buildGradle("""
                plugins {
                    id("com.cognifide.aem.bundle")
                }
                
                group = "com.company.example"
                
                repositories {
                    jcenter()
                }
                
                dependencies {
                    compileOnly("org.slf4j:slf4j-api:1.5.10")
                    compileOnly("org.osgi:osgi.cmpn:6.0.0")
                }
                """)

            helloServiceJava()
        }

        runBuild(projectDir, "bundleCompose", "-Poffline") {
            assertTask(":bundleCompose")
            assertBundle("build/bundleCompose/bundle-minimal.jar")
            assertZipEntryEquals("build/bundleCompose/bundle-minimal.jar", "OSGI-INF/com.company.example.aem.HelloService.xml", """
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
    fun `should build package with bundle using extended configuration`() {
        val projectDir = prepareProject("bundle-extended") {
            /**
             * This is not required here but it proves that there is some issue with Gradle TestKit;
             * This generated project works when running using Gradle Wrapper.
             */
            settingsGradle("""
                pluginManagement {
                    plugins {
                        repositories {
                            mavenLocal()
                            jcenter()
                            gradlePluginPortal()
                        }
                    } 
                } 
            """)

            buildGradle("""
                plugins {
                    id("com.cognifide.aem.bundle")
                    id("maven-publish")
                }
                
                group = "com.company.example"
                version = "1.0.0"
                
                repositories {
                    jcenter()
                    maven("https://repo.adobe.com/nexus/content/groups/public")
                }
                
                dependencies {
                    compileOnly("org.slf4j:slf4j-api:1.5.10")
                    compileOnly("org.osgi:osgi.cmpn:6.0.0")
                    compileOnly("com.adobe.aem:uber-jar:6.5.0:apis")
                    
                    testImplementation("org.junit.jupiter:junit-jupiter:5.5.2")
                    testImplementation("io.wcm:io.wcm.testing.aem-mock.junit5:2.5.2")
                }

                tasks {
                    bundleCompose {
                        category = "example"
                        vendor = "Company"
                    }
                }
                
                publishing {
                    publications {
                        create<MavenPublication>("mavenJava") {
                            from(components["java"])
                        }
                    }
                }
                """)

            helloServiceJava()
        }

        runBuild(projectDir, "bundleCompose", "-Poffline") {
            assertTask(":bundleCompose")
            assertBundle("build/bundleCompose/bundle-extended-1.0.0.jar")
        }

        runBuild(projectDir, "publishToMavenLocal", "-Poffline") {
            assertTask(":bundleCompose", TaskOutcome.UP_TO_DATE)

            val mavenDir = File("${System.getProperty("user.home")}/.m2/repository/com/company/example/bundle-extended/1.0.0")
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
                plugins {
                    id("com.cognifide.aem.bundle")
                }
                
                group = "com.company.example"
                
                repositories {
                    jcenter()
                }
                
                dependencies {
                    compileOnly("org.slf4j:slf4j-api:1.5.10")
                    compileOnly("org.osgi:osgi.cmpn:6.0.0")
                }
                
                aem {
                    tasks {
                        bundleCompose {
                            embedPackage("org.hashids:hashids:1.0.1", "org.hashids")
                        }
                    }
                }
                """)

            helloServiceJava()
        }

        runBuild(projectDir, "bundleCompose", "-Poffline") {
            assertTask(":bundleCompose")

            val jar = file("build/bundleCompose/bundle-embed.jar")

            assertBundle(jar)

            assertZipEntry(jar, "OSGI-INF/com.company.example.aem.HelloService.xml")
            assertZipEntry(jar, "com/company/example/aem/HelloService.class")
            assertZipEntry(jar, "org/hashids/Hashids.class")
        }
    }
}
