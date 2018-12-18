import com.cognifide.gradle.aem.pkg.tasks.Compose

plugins {
    id("com.cognifide.aem.bundle")
    kotlin("jvm")
}

description = "Example - Common"

dependencies {
    compileOnly("org.hashids:hashids:1.0.1")
}

aem {
    tasks {
        compose {
            fromJar("org.jetbrains.kotlin:kotlin-osgi-bundle:1.2.21")
        }
        bundle {
            javaPackage = "com.company.example.aem.common"
            exportPackage("org.hashids")
        }
    }
}