plugins {
    id("com.cognifide.aem.bundle")
    kotlin("jvm")
}

description = "Example - Core"

dependencies {
    compile(project(":common"))
}