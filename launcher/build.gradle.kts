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
        manifest {
            attributes["Implementation-Title"] = project.description
            attributes["Main-Class"] = "com.cognifide.gradle.aem.launcher.Launcher"
        }
        from(configurations.runtimeClasspath.get().files.map { if (it.isDirectory) it else zipTree(it) })
        from(project.provider {
            val dependency = project.dependencies.create(project.dependencies.project(":"))
            configurations.detachedConfiguration(dependency).apply { isTransitive = false }
        }) {
            rename { "gap.jar" }
        }
    }
    register<Test>("integTest") {
        testClassesDirs = integTestSourceSet.output.classesDirs
        classpath = integTestSourceSet.runtimeClasspath

        useJUnitPlatform()
        mustRunAfter("test")
        dependsOn(jar/*, "detektIntegTest"*/)
        outputs.dir("build/integTest")
    }
    named<Task>("build") {
        dependsOn("standaloneJar")
    }
}