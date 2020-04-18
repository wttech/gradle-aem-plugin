import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
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

    "integTestRuntimeOnly"("org.junit.jupiter:junit-jupiter-engine:5.3.2")
    "integTestImplementation"("org.junit.jupiter:junit-jupiter-api:5.3.2")
    "integTestImplementation"("org.buildobjects:jproc:2.3.0")
}

tasks {
    withType<KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = "1.8"
    }
    val standaloneJar = register<Jar>("standaloneJar") {
        manifest {
            attributes["Implementation-Title"] = project.description
            attributes["Main-Class"] = "com.cognifide.gradle.aem.launcher.LauncherKt"
        }
        from(configurations.runtimeClasspath.get().files.map { if (it.isDirectory) it else zipTree(it) })
        with(named<Jar>("jar").get())
    }
    register<Test>("integTest") {
        testClassesDirs = integTestSourceSet.output.classesDirs
        classpath = integTestSourceSet.runtimeClasspath

        useJUnitPlatform()
        mustRunAfter("test")
        dependsOn(standaloneJar/*, "detektIntegTest"*/)
        outputs.dir("build/integTest")
    }
    named<Task>("build") {
        dependsOn("standaloneJar")
    }
}