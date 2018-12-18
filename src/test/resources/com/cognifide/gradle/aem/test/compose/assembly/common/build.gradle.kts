import com.cognifide.gradle.aem.pkg.tasks.Compose

plugins {
    id("com.cognifide.aem.bundle")
    kotlin("jvm")
}

description = "Example - Common"

aem {
    tasks {
        compose {
            fromJar("org.jetbrains.kotlin:kotlin-osgi-bundle:1.2.21")
        }
        bundle {
            embedPackage("org.hashids", true, "org.hashids:hashids:1.0.1")
        }
    }
}