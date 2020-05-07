import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    `maven-publish`
}

group = "com.cognifide.gradle"
description = "Gradle AEM Plugin - Standalone Launcher"
defaultTasks("build", "publishToMavenLocal")

repositories {
    mavenLocal()
    jcenter()
    maven("https://repo.gradle.org/gradle/libs-releases-local")
}

val integTestSourceSet = sourceSets.create("integTest")
configurations.getByName("integTestImplementation").apply {
    extendsFrom(configurations.getByName("testImplementation"))
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.gradle:gradle-tooling-api:6.3")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.10.3")
    runtimeOnly("org.slf4j:slf4j-simple:1.7.10")

    "integTestRuntimeOnly"("org.junit.jupiter:junit-jupiter-engine:5.3.2")
    "integTestImplementation"("org.junit.jupiter:junit-jupiter-api:5.3.2")
    "integTestImplementation"("org.buildobjects:jproc:2.3.0")
}

tasks {
    withType<KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = "1.8"
    }
    jar {
        dependsOn(":processResources", ":publishToMavenLocal")
        from(rootProject.buildDir.resolve("resources/main/build.json"))
        manifest {
            attributes["Implementation-Title"] = project.description
            attributes["Main-Class"] = "com.cognifide.gradle.aem.launcher.Launcher"
        }
        from(configurations.runtimeClasspath.get().files.map { if (it.isDirectory) it else zipTree(it) })
    }
    register<Test>("integTest") {
        testClassesDirs = integTestSourceSet.output.classesDirs
        classpath = integTestSourceSet.runtimeClasspath

        useJUnitPlatform()
        mustRunAfter("test")
        dependsOn(jar/*, "detektIntegTest"*/)
        outputs.dir("build/integTest")
    }
}