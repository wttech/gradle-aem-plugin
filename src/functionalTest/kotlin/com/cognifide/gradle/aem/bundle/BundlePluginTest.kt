package com.cognifide.gradle.aem.bundle
import com.cognifide.gradle.aem.test.AemBuildTest
import org.junit.jupiter.api.Test

class BundlePluginTest: AemBuildTest() {

    @Test
    fun `should build package with bundle using minimal configuration`() {
        // given
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
            assertBundle("build/aem/bundleCompose/bundle.minimal.jar")
        }
    }

    @Test
    fun `should build package with bundle using extended configuration`() {
        // given
        val projectDir = prepareProject("bundle-extended") {
            settingsGradle("")

            buildGradle("""
                plugins {
                    id("com.cognifide.aem.bundle")
                }
                
                group = "com.company.example"
                version = "1.0.0"
                
                repositories {
                    jcenter()
                    maven { url = uri("https://repo.adobe.com/nexus/content/groups/public") }
                }
                
                dependencies {
                    compileOnly("org.slf4j:slf4j-api:1.5.10")
                    compileOnly("org.osgi:osgi.cmpn:6.0.0")
                    compileOnly("com.adobe.aem:uber-jar:6.5.0:apis")
                }
                
                aem {
                    tasks {
                        bundleCompose {
                            category = "example"
                            vendor = "Company"
                        }
                    }
                }
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
            assertBundle("build/aem/bundleCompose/bundle.extended-1.0.0.jar")
        }

        runBuild(projectDir, "packageCompose", "-Poffline") {
            assertTask(":packageCompose")
            assertPackage("build/aem/packageCompose/bundle.extended-1.0.0.zip")
        }
    }
}