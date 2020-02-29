package com.cognifide.gradle.aem.bundle

import com.cognifide.gradle.aem.test.AemBuildTest
import org.junit.jupiter.api.Test

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
                }

                tasks {
                    bundleCompose {
                        category = "example"
                        vendor = "Company"
                    }
                }
                """)

            helloServiceJava()
        }

        runBuild(projectDir, "bundleCompose", "-Poffline") {
            assertTask(":bundleCompose")
            assertBundle("build/bundleCompose/bundle-extended-1.0.0.jar")
        }

        runBuild(projectDir, "packageCompose", "-Poffline") {
            assertTask(":packageCompose")

            val pkgPath = "build/packageCompose/bundle-extended-1.0.0.zip"

            assertPackage(pkgPath)
            assertPackageBundle(pkgPath, "jcr_root/apps/bundle-extended/install/bundle-extended-1.0.0.jar")
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
