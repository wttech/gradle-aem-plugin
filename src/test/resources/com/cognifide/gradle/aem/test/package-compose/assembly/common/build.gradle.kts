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
        bundleCompose {
            exportPackage("org.hashids")
        }
        packageCompose {
            fromJar("org.jetbrains.kotlin:kotlin-osgi-bundle:1.2.21")
        }
    }
}
