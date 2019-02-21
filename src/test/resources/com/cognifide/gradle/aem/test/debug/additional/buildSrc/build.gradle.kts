repositories {
    jcenter()
    mavenLocal()
    maven { url = uri("http://dl.bintray.com/cognifide/maven-public") }
}

dependencies {
    implementation("com.cognifide.gradle:aem-plugin:6.1.0-beta")
}
