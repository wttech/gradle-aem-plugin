plugins {
    id("com.cognifide.aem.package")
}

description = "Example"

allprojects {
    group = "com.company.aem"
    version = "1.0.0-SNAPSHOT"

    repositories {
        mavenLocal()
        jcenter()
        maven { url = uri("https://repo.adobe.com/nexus/content/groups/public") }
        maven { url = uri("https://repo1.maven.org/maven2") }
    }

    plugins.withId("com.cognifide.aem.common") {
        aem {
            instance {
                local("http://localhost:4502")
                local("http://localhost:4503")
            }
        }
    }

    plugins.withId("com.cognifide.aem.bundle") {
        aem {
            tasks {
                bundleCompose {
                    category = "example"
                    vendor = "Company"
                }
            }
        }

        dependencies {
            "compileOnly"("org.slf4j:slf4j-api:1.5.10")
            "compileOnly"("org.osgi:osgi.cmpn:6.0.0")
        }
    }
}

aem {
    tasks {
        packageCompose {
            fromProject(":common")
            fromProject(":core")
            fromProject(":design")
        }
    }
}
