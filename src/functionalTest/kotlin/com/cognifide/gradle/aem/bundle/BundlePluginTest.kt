package com.cognifide.gradle.aem.bundle
import com.cognifide.gradle.aem.test.BaseTest
import org.junit.jupiter.api.Test

class BundlePluginTest: BaseTest() {

    @Test
    fun `should build package with bundle using minimal configuration`() {
        // given
        val projectDir = projectDir("bundle/minimal") {
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

        // when
        val buildResult = runBuild(projectDir, "packageCompose", "-Poffline")

        // then
        assertTask(buildResult, ":packageCompose")
    }

    @Test
    fun `should build package with bundle using extended configuration`() {
        // given
        val projectDir = projectDir("bundle/extended") {
            settingsGradle("")

            buildGradle("""
                plugins {
                    id("com.cognifide.aem.bundle")
                }
                
                group = "com.company.example"
                
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

        // when
        val bundleBuildResult = runBuild(projectDir, "bundleCompose", "-Poffline")

        // then
        assertTask(bundleBuildResult, ":bundleCompose")

        // when
        val packageBuildResult = runBuild(projectDir, "packageCompose", "-Poffline")

        // then
        assertTask(packageBuildResult, ":packageCompose")
    }
}