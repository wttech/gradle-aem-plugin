plugins {
    id("com.cognifide.aem.bundle")
    id("kotlin")
}

description = "Example - Core"


aem {
    config {
        bundlePackage = "com.company.aem.example.core"
    }
}

dependencies {
    compile(project(":common"))
}
