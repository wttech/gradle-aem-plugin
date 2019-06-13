plugins {
    id("com.cognifide.aem.bundle")
}

group = "com.company.aem"
version = "1.0.0-SNAPSHOT"
description = "Example"

repositories {
    mavenLocal()
    jcenter()
    maven { url = uri("https://repo.adobe.com/nexus/content/groups/public") }
    maven { url = uri("https://repo1.maven.org/maven2") }
}

dependencies {
    compileOnly("org.slf4j:slf4j-api:1.5.10")
    compileOnly("org.osgi:osgi.cmpn:6.0.0")
}

aem {
    tasks {
        bundle {
            category = "example"
            vendor = "Company"
        }
    }
}