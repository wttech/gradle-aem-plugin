pluginManagement {
    repositories {
        jcenter()
        mavenLocal()
        maven { url = uri("http://dl.bintray.com/cognifide/maven-public") }
    }
    resolutionStrategy {
        eachPlugin {
            when {
                requested.id.namespace == "com.cognifide.aem" -> useModule("com.cognifide.gradle:aem-plugin:6.0.0")
            }
        }
    }
}

rootProject.name = "minimal"