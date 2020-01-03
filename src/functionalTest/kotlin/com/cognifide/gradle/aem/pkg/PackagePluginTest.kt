package com.cognifide.gradle.aem.pkg
import com.cognifide.gradle.aem.test.AemBuildTest
import org.junit.jupiter.api.Test

class PackagePluginTest: AemBuildTest() {

    @Test
    fun `should build package using minimal configuration`() {
        val projectDir = prepareProject("package-minimal") {
            settingsGradle("")

            buildGradle("""
                plugins {
                    id("com.cognifide.aem.package")
                }
                
                group = "com.company.example"
                version = "1.0.0"
                """)

            file("src/main/content/jcr_root/apps/example/.content.xml", """
                <?xml version="1.0" encoding="UTF-8"?>
                <jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0" xmlns:jcr="http://www.jcp.org/jcr/1.0"
                    jcr:primaryType="sling:Folder"/>
                """)

            file("src/main/content/META-INF/vault/filter.xml", """
                <?xml version="1.0" encoding="UTF-8"?>
                <workspaceFilter version="1.0">
                    <filter root="/apps/example"/>
                </workspaceFilter>
                """)
        }

        runBuild(projectDir, "packageCompose", "-Poffline") {
            assertTask(":packageCompose")

            val pkgPath = "build/aem/packageCompose/package.minimal-1.0.0.zip"

            assertPackage(pkgPath)

            assertZipEntry(pkgPath, "jcr_root/apps/example/.content.xml")

            assertZipEntry(pkgPath, "META-INF/vault/filter.xml", """
                <?xml version="1.0" encoding="UTF-8"?>
                <workspaceFilter version="1.0">
                  
                  <filter root="/apps/example"></filter>
                  
                </workspaceFilter>

            """)

            assertZipEntry(pkgPath, "META-INF/vault/properties.xml", """
                <?xml version="1.0" encoding="UTF-8" standalone="no"?>
                <!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">
                <properties>
                    
                    <entry key="group">com.company.example</entry>
                    <entry key="name">package.minimal</entry>
                    <entry key="version">1.0.0</entry>
                    
                    <entry key="createdBy">${System.getProperty("user.name")}</entry>
                    
                    
                    <entry key="acHandling">merge_preserve</entry>
                    
                    <entry key="requiresRoot">false</entry>
                    
                </properties>
            """)
        }
    }
}