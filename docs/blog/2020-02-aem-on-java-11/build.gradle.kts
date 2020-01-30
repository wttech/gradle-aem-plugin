plugins {
    id("com.cognifide.aem.instance")
}

repositories {
    jcenter()
}

aem {
    tasks {
        instanceProvision {
            step("enable-crxde") {
                description = "Enables CRX DE"
                condition { once() && instance.author }
                action {
                    sync {
                        osgiFramework.configure(
                                "org.apache.sling.jcr.davex.impl.servlets.SlingDavExServlet",
                                mapOf("alias" to "/crx/server")
                        )
                    }
                }
            }
        }
        instanceSatisfy {
            packages {
                "dep.acs-aem-commons"("https://github.com/Adobe-Consulting-Services/acs-aem-commons/releases/download/acs-aem-commons-4.0.0/acs-aem-commons-content-4.0.0-min.zip")
                "dep.kotlin"("org.jetbrains.kotlin:kotlin-osgi-bundle:1.3.61")
                "dep.groovy-console"("https://github.com/icfnext/aem-groovy-console/releases/download/13.0.0/aem-groovy-console-13.0.0.zip")
            }
        }
    }
}