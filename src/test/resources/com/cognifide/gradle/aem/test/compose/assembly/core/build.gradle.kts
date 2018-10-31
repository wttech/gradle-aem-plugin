plugins {
    id("com.cognifide.aem.bundle")
    kotlin("jvm")
}

description = "Example - Core"


aem {
    bundle {
        javaPackage = "com.company.aem.example.core"
    }
}

dependencies {
    compile(project(":common"))
}
