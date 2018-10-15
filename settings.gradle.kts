pluginManagement {
    repositories {
        jcenter()
        gradlePluginPortal()
        maven { url = uri("https://dl.bintray.com/kotlin/kotlin-eap") }
    }
}

rootProject.name = "aem-plugin"
enableFeaturePreview("STABLE_PUBLISHING")