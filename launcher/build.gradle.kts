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

val functionalTestSourceSet = sourceSets.create("functionalTest")
configurations.getByName("functionalTestImplementation").apply {
    extendsFrom(configurations.getByName("testImplementation"))
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.gradle:gradle-tooling-api:6.3")
    runtimeOnly("org.slf4j:slf4j-simple:1.7.10")

    "functionalTestRuntimeOnly"("org.junit.jupiter:junit-jupiter-engine:5.3.2")
    "functionalTestImplementation"("org.junit.jupiter:junit-jupiter-api:5.3.2")
    "functionalTestImplementation"("org.buildobjects:jproc:2.3.0")
}

tasks {
    withType<KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = "1.8"
    }
    val buildProperties = register("buildProperties") {
        val properties = """
            pluginVersion=${project.version}
            gradleVersion=${project.gradle.gradleVersion}
        """.trimIndent()
        val file = file("$buildDir/resources/main/build.properties")

        inputs.property("buildProperties", properties)
        outputs.file(file)
        doLast { file.writeText(properties) }
    }
    jar {
        dependsOn(buildProperties, ":publishToMavenLocal")
        manifest {
            attributes["Implementation-Title"] = project.description
            attributes["Main-Class"] = "com.cognifide.gradle.aem.launcher.Launcher"
        }
        from(configurations.runtimeClasspath.get().files.map { if (it.isDirectory) it else zipTree(it) })
        archiveFileName.set("gap.jar")
    }
    register<Test>("functionalTest") {
        testClassesDirs = functionalTestSourceSet.output.classesDirs
        classpath = functionalTestSourceSet.runtimeClasspath

        useJUnitPlatform()
        mustRunAfter("test")
        dependsOn(jar)
        outputs.dir("build/functionalTest")
    }
    publishToMavenLocal {
        dependsOn(jar)
    }
}