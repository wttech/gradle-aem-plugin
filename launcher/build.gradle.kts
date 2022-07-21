import io.gitlab.arturbosch.detekt.Detekt

plugins {
    kotlin("jvm")
    `maven-publish`
    id("io.gitlab.arturbosch.detekt") version "1.20.0-RC1"
}

group = "com.cognifide.gradle"
description = "Gradle AEM Plugin - Standalone Launcher"
defaultTasks("build", "publishToMavenLocal")

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.gradle.org/gradle/libs-releases")
}

val functionalTestSourceSet = sourceSets.create("functionalTest")
configurations.getByName("functionalTestImplementation").apply {
    extendsFrom(configurations.getByName("testImplementation"))
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.gradle:gradle-tooling-api:7.4-rc-1")
    runtimeOnly("org.slf4j:slf4j-simple:1.7.10")

    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.20.0-RC1")

    "functionalTestRuntimeOnly"("org.junit.jupiter:junit-jupiter-engine:5.3.2")
    "functionalTestImplementation"("org.junit.jupiter:junit-jupiter-api:5.3.2")
    "functionalTestImplementation"("org.buildobjects:jproc:2.3.0")

}

tasks {
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

    withType<Detekt>().configureEach {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }

    jar {
        dependsOn(buildProperties, ":publishToMavenLocal")
        manifest {
            attributes["Implementation-Title"] = project.description
            attributes["Main-Class"] = "com.cognifide.gradle.aem.launcher.Launcher"
        }
        from(configurations.runtimeClasspath.get().files.map { if (it.isDirectory) it else zipTree(it) })
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        archiveFileName.set("gap.jar")
    }

    test {
        dependsOn(buildProperties, "detektTest")
    }

    register<Test>("functionalTest") {
        testClassesDirs = functionalTestSourceSet.output.classesDirs
        classpath = functionalTestSourceSet.runtimeClasspath
        testLogging.showStandardStreams = true
        useJUnitPlatform()
        mustRunAfter("test")
        dependsOn(jar)
        outputs.dir("build/functionalTest")
    }
    publishToMavenLocal {
        dependsOn(jar)
    }
}

detekt {
    config.from(rootProject.file("detekt.yml"))
    parallel = true
    autoCorrect = true
}
