import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.dokka")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish")
    id("maven-publish")
    id("io.gitlab.arturbosch.detekt")
    id("com.jfrog.bintray")
    id("net.researchgate.release")
    id("com.github.breadmoirai.github-release")
}

group = "com.cognifide.gradle"
description = "Gradle AEM Plugin"
defaultTasks("build", "publishToMavenLocal")

repositories {
    jcenter()
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.3.61"))

    implementation(gradleApi())
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.2")
    implementation("org.apache.commons:commons-lang3:3.4")
    implementation("commons-io:commons-io:2.4")
    implementation("commons-validator:commons-validator:1.6")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.10.0")
    implementation("org.reflections:reflections:0.9.9")
    implementation("org.apache.jackrabbit.vault:vault-cli:3.2.4")
    implementation("org.jsoup:jsoup:1.10.3")
    implementation("org.samba.jcifs:jcifs:1.3.18-kohsuke-1")
    implementation("biz.aQute.bnd:biz.aQute.bnd.gradle:4.2.0")
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
    implementation("net.adamcin.oakpal:oakpal-core:1.5.1")

    testImplementation("org.junit.jupiter:junit-jupiter:5.5.2")

    "detektPlugins"("io.gitlab.arturbosch.detekt:detekt-formatting:1.0.1")
}

val functionalTestSourceSet = sourceSets.create("functionalTest") {}
gradlePlugin.testSourceSets(functionalTestSourceSet)
configurations.getByName("functionalTestImplementation").extendsFrom(configurations.getByName("testImplementation"))


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
        outputFormat = "html"
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
            freeCompilerArgs = freeCompilerArgs + "-Xuse-experimental=kotlin.Experimental"
        }
    }
    register<Test>("functionalTest") {
        testClassesDirs = functionalTestSourceSet.output.classesDirs
        classpath = functionalTestSourceSet.runtimeClasspath

        systemProperties(System.getProperties().asSequence().map {
            it.key.toString() to it.value.toString() }.filter {
                it.first.run { startsWith("fileTransfer.") || startsWith("localInstance.") }
            }.toMap()
        )

        useJUnitPlatform()
        mustRunAfter("test")
        dependsOn("jar")
        outputs.upToDateWhen { false }
    }
    named<Task>("build") {
        dependsOn("sourcesJar", "javadocJar")
    }

    named<Task>("publishToMavenLocal") {
        dependsOn("sourcesJar", "javadocJar")
    }

    named<ProcessResources>("processResources") {
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

    named("afterReleaseBuild") {
        dependsOn("bintrayUpload", "publishPlugins")
    }

    named("githubRelease") {
        dependsOn("release")
    }

    register("fullRelease") {
        dependsOn("release", "githubRelease")
    }

    withType<Test>().configureEach {
        testLogging.showStandardStreams = true
        useJUnitPlatform()
    }
}

detekt {
    config.from(file("detekt.yml"))
    parallel = true
    autoCorrect = true
    failFast = true
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(tasks["sourcesJar"])
            artifact(tasks["javadocJar"])
        }
    }
}

gradlePlugin {
    plugins {
        create("common") {
            id = "com.cognifide.aem.common"
            implementationClass = "com.cognifide.gradle.aem.common.CommonPlugin"
            displayName = "AEM Common Plugin"
            description = "Provides AEM DSL extension to build script on which all other logic is based."
        }
        create("package") {
            id = "com.cognifide.aem.package"
            implementationClass = "com.cognifide.gradle.aem.pkg.PackagePlugin"
            displayName = "AEM Package Plugin"
            description = "Provides tasks for working with CRX packages."
        }
        create("package.sync") {
            id = "com.cognifide.aem.package.sync"
            implementationClass = "com.cognifide.gradle.aem.pkg.PackageSyncPlugin"
            displayName = "AEM Package Sync Plugin"
            description = "Provides tasks for synchronizing JCR content from running AEM instance."
        }
        create("bundle") {
            id = "com.cognifide.aem.bundle"
            implementationClass = "com.cognifide.gradle.aem.bundle.BundlePlugin"
            displayName = "AEM Bundle Plugin"
            description = "Adds support for building OSGi bundles."
        }
        create("instance") {
            id = "com.cognifide.aem.instance"
            implementationClass = "com.cognifide.gradle.aem.instance.InstancePlugin"
            displayName = "AEM Instance Plugin"
            description = "Provides tasks for working with native local AEM instances."
        }
        create("environment") {
            id = "com.cognifide.aem.environment"
            implementationClass = "com.cognifide.gradle.aem.environment.EnvironmentPlugin"
            displayName = "AEM Environment Plugin"
            description = "Provides tasks for working with virtualized AEM environment."
        }
    }
}

pluginBundle {
    website = "https://github.com/Cognifide/gradle-aem-plugin"
    vcsUrl = "https://github.com/Cognifide/gradle-aem-plugin.git"
    description = "Gradle AEM Plugin"
    tags = listOf("aem", "cq", "vault", "scr")
}

bintray {
    user = (project.findProperty("bintray.user") ?: System.getenv("BINTRAY_USER"))?.toString()
    key = (project.findProperty("bintray.key") ?: System.getenv("BINTRAY_KEY"))?.toString()
    setPublications("mavenJava")
    with(pkg) {
        repo = "maven-public"
        name = "gradle-aem-plugin"
        userOrg = "cognifide"
        setLicenses("Apache-2.0")
        vcsUrl = "https://github.com/Cognifide/gradle-aem-plugin.git"
        setLabels("aem", "cq", "vault", "scr")
        with(version) {
            name = project.version.toString()
            desc = "${project.description} ${project.version}"
            vcsTag = project.version.toString()
        }
    }
    publish = (project.findProperty("bintray.publish") ?: "true").toString().toBoolean()
    override = (project.findProperty("bintray.override") ?: "false").toString().toBoolean()
}

githubRelease {
    owner("Cognifide")
    repo("gradle-aem-plugin")
    token((project.findProperty("github.token") ?: "").toString())
    tagName(project.version.toString())
    releaseName(project.version.toString())
    releaseAssets(tasks["jar"], tasks["sourcesJar"], tasks["javadocJar"])
    draft((project.findProperty("github.draft") ?: "false").toString().toBoolean())
    prerelease((project.findProperty("github.prerelease") ?: "false").toString().toBoolean())
    overwrite((project.findProperty("github.override") ?: "true").toString().toBoolean())

    body { """
    |# What's new
    |
    |TBD
    |
    |# Upgrade notes
    |
    |Nothing to do.
    |
    |# Contributions
    |
    |None.
    """.trimMargin()
    }
}
