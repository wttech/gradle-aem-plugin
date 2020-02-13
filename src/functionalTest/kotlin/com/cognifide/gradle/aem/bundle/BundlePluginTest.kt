package com.cognifide.gradle.aem.bundle

import com.cognifide.gradle.aem.test.AemBuildTest
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class BundlePluginTest: AemBuildTest() {

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

            file("src/main/java/com/company/example/HelloService.java", """
                package com.company.aem.example;
                
                import org.osgi.service.component.annotations.Activate;
                import org.osgi.service.component.annotations.Component;
                import org.osgi.service.component.annotations.Deactivate;
                import org.slf4j.Logger;
                import org.slf4j.LoggerFactory;
                
                @Component(immediate = true, service = HelloService.class)
                class HelloService {
                                   
                    private static final Logger LOG = LoggerFactory.getLogger(HelloService.class);
                    
                    @Activate
                    protected void activate() {
                        LOG.info("Hello world!");
                    }
                    
                    @Deactivate
                    protected void deactivate() {
                        LOG.info("Good bye world!");
                    }
                }
                """)
        }

        runBuild(projectDir, "bundleCompose", "-Poffline") {
            assertTask(":bundleCompose")
            assertBundle("build/bundleCompose/bundle-minimal.jar")
            assertZipEntry("build/bundleCompose/bundle-minimal.jar", "OSGI-INF/com.company.aem.example.HelloService.xml", """
                <?xml version="1.0" encoding="UTF-8"?>
                <scr:component xmlns:scr="http://www.osgi.org/xmlns/scr/v1.3.0" name="com.company.aem.example.HelloService" immediate="true" activate="activate" deactivate="deactivate">
                  <service>
                    <provide interface="com.company.aem.example.HelloService"/>
                  </service>
                  <implementation class="com.company.aem.example.HelloService"/>
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
                
                /*
                tasks {
                    bundleCompose { // this line breaks build when running using Gradle TestKit
                        category = "example"
                        vendor = "Company"
                    }
                }
                */
                
                /*
                ScriptCompilationException(errors=[ScriptCompilationError(message=Supertypes of the following classes cannot be resolved. Please make sure you have the required dependencies in the classpath:
                    class com.cognifide.gradle.aem.bundle.tasks.BundleCompose, unresolved supertypes: com.cognifide.gradle.common.tasks.JarTask
                , location=null)])
                    at org.gradle.kotlin.dsl.support.KotlinCompilerKt.compileKotlinScriptModuleTo(KotlinCompiler.kt:175)
                    at org.gradle.kotlin.dsl.support.KotlinCompilerKt.compileKotlinScriptToDirectory(KotlinCompiler.kt:135)
                    at org.gradle.kotlin.dsl.execution.ResidualProgramCompiler$ compileScript1.invoke(ResidualProgramCompiler.k
                 */
                """)

            file("src/main/java/com/company/example/PageService.java", """
                package com.company.aem.example;
                
                import org.osgi.service.component.annotations.Activate;
                import org.osgi.service.component.annotations.Component;
                import org.osgi.service.component.annotations.Deactivate;
                import org.osgi.service.component.annotations.Reference;
                import com.day.cq.wcm.api.PageManager;
                import com.day.cq.wcm.api.Page;
                
                @Component(immediate = true, service = PageService.class)
                class PageService {
    
                    @Reference
                    private PageManager pageManager;
                    
                    public Page getHomePage() {
                        return pageManager.getPage("/content/example/home");
                    }
                }
                """)
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
}