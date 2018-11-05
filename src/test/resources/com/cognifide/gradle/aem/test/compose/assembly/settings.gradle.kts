// Plugins configuration

pluginManagement {
    repositories {
        mavenLocal()
        jcenter()
    }

    resolutionStrategy {
        eachPlugin {
            when {
                requested.id.namespace == "com.cognifide.aem" -> useModule("com.cognifide.gradle:aem-plugin:6.0.0")
                requested.id.id == "org.jetbrains.kotlin.jvm" -> useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.0")
            }
        }
    }
}

// Project structure

rootProject.name = "example"

include("common")
include("core")
include("design")