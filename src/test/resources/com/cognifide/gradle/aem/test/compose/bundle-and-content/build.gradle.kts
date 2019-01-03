plugins {
    id("com.cognifide.aem.bundle")
}

group = "com.company.aem"
version = "1.0.0-SNAPSHOT"
description = "Example"

repositories {
    mavenLocal()
    maven { url = uri("https://repo.adobe.com/nexus/content/groups/public") }
    maven { url = uri("https://repo1.maven.org/maven2") }
    jcenter()
}

dependencies {
    compile(group = "org.slf4j", name = "slf4j-api", version = "1.5.10")
    compile(group = "org.osgi", name = "osgi.cmpn", version = "6.0.0")
}

aem {
    tasks {
        bundle {
            category = "example"
            vendor = "Company"
        }
    }
}