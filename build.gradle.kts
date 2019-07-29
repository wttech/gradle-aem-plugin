import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.dokka")
    id("java-gradle-plugin")
    id("maven-publish")
    id("io.gitlab.arturbosch.detekt")
    id("com.jfrog.bintray")
    id("com.neva.fork")
    id("net.researchgate.release")
}

group = "com.cognifide.gradle"
description = "Gradle AEM Plugin"
defaultTasks("build", "publishToMavenLocal")

repositories {
    jcenter()
}

dependencies {
    implementation(gradleApi())
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.3.41")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.3.41")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.2.0")
    implementation("org.apache.commons:commons-lang3:3.4")
    implementation("commons-io:commons-io:2.4")
    implementation("commons-validator:commons-validator:1.6")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.8.8")
    implementation("org.reflections:reflections:0.9.9")
    implementation("org.apache.jackrabbit.vault:vault-cli:3.2.4")
    implementation("org.jsoup:jsoup:1.10.3")
    implementation("org.samba.jcifs:jcifs:1.3.18-kohsuke-1")
    implementation("biz.aQute.bnd:biz.aQute.bnd.gradle:4.0.0")
    implementation("org.zeroturnaround:zt-zip:1.11")
    implementation("net.lingala.zip4j:zip4j:1.3.2")
    implementation("org.apache.sshd:sshd-sftp:2.2.0")
    implementation("org.apache.httpcomponents:httpclient:4.5.4")
    implementation("org.apache.httpcomponents:httpmime:4.5.4")
    implementation("org.osgi:org.osgi.core:6.0.0")
    implementation("io.pebbletemplates:pebble:3.0.4")
    implementation("com.dorkbox:Notify:3.7")
    implementation("com.jayway.jsonpath:json-path:2.4.0")
    implementation("org.buildobjects:jproc:2.2.3")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.3.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.3.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.3.1")
    testImplementation(gradleTestKit())
    testImplementation("org.skyscreamer:jsonassert:1.5.0")
    testImplementation("org.junit-pioneer:junit-pioneer:0.2.2")

    "detektPlugins"("io.gitlab.arturbosch.detekt:detekt-formatting:1.0.0-RC16")
}

tasks {

    register<Zip>("tailerZip") {
        from("dists/gradle-aem-tailer")

        archiveFileName.set("gradle-aem-tailer.zip")
        destinationDirectory.set(file("dists"))
    }

    register<Jar>("sourcesJar") {
        archiveClassifier.set("sources")
        dependsOn("classes")
        from(sourceSets["main"].allSource)
    }

    register<DokkaTask>("dokkaJavadoc") {
        outputFormat = "javadoc"
        outputDirectory = "$buildDir/javadoc"
    }

    register<Jar>("javadocJar") {
        archiveClassifier.set("javadoc")
        dependsOn("dokkaJavadoc")
        from("$buildDir/javadoc")
    }

    withType<JavaCompile>().configureEach{
        sourceCompatibility = JavaVersion.VERSION_1_8.toString()
        targetCompatibility = JavaVersion.VERSION_1_8.toString()
    }

    withType<KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = JavaVersion.VERSION_1_8.toString()
        }
    }

    named<Task>("build") {
        dependsOn("sourcesJar", "javadocJar")
    }

    named<Task>("publishToMavenLocal") {
        dependsOn("sourcesJar", "javadocJar")
    }

    named<ProcessResources>("processResources") {
        dependsOn( "tailerZip")

        val json = """
        |{
        |    "pluginVersion": "${project.version}",
        |    "gradleVersion": "${project.gradle.gradleVersion}"
        |}""".trimMargin()
        val file = file("$buildDir/resources/main/build.json")

        inputs.property("buildJson", json)
        outputs.file(file)

        doLast {
            file.writeText(json)
        }
    }

    named<Test>("test") {
        testLogging {
            events = setOf(TestLogEvent.FAILED)
            exceptionFormat = TestExceptionFormat.SHORT
        }

        useJUnitPlatform()
        dependsOn(named("publishToMavenLocal"))
    }

    named("afterReleaseBuild") {
        dependsOn("bintrayUpload")
    }

    named("updateVersion") {
        enabled = false
    }
}

detekt {
    config.from(file("detekt.yml"))
}

apply(from = "gradle/publish.gradle.kts")
apply(from = "gradle/fork.gradle.kts")