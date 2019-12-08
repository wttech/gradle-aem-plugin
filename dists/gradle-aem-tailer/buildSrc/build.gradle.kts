repositories {
    mavenLocal()
    jcenter()
    maven { url = uri("https://plugins.gradle.org/m2") }
    maven { url = uri("http://dl.bintray.com/cognifide/maven-public") }
}

dependencies {
    implementation("com.cognifide.gradle:aem-plugin:9.1.5")
}
