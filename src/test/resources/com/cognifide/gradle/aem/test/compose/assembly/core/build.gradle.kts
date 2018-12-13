plugins {
    id("com.cognifide.aem.bundle")
    kotlin("jvm")
}

description = "Example - Core"


aem {
    bundle {
        javaPackage = "com.compan.example.aem.core"
    }
}

dependencies {
    compile(project(":common"))
}
