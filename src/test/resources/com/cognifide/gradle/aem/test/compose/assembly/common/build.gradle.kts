plugins {
    id("com.cognifide.aem.bundle")
    id("kotlin")
}

description = "Example - Common"

dependencies {
    `aemInstall`(group = "org.jetbrains.kotlin", name = "kotlin-osgi-bundle", version = "1.2.21")
    `aemEmbed`(group = "org.hashids", name = "hashids", version = "1.0.1")
}

aem {
    config {
        bundlePackage = "com.company.aem.example.common"
    }
    bundle {
        exportPackage("org.hashids")
    }
}