import com.cognifide.gradle.aem.pkg.tasks.Compose

plugins {
    id("com.cognifide.aem.bundle")
    kotlin("jvm")
}

description = "Example - Common"

tasks.named<Compose>("aemCompose") {
    fromJar("org.jetbrains.kotlin:kotlin-osgi-bundle:1.2.21")
}

dependencies {
    compileOnly("org.hashids:hashids:1.0.1")
}

aem {
    bundle {
        javaPackage = "com.company.example.aem.common"
        exportPackage("org.hashids")
    }
}