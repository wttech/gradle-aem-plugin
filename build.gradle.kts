import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import io.gitlab.arturbosch.detekt.Detekt

plugins {
    id("java-gradle-plugin")
    id("maven-publish")
    id("org.jetbrains.kotlin.jvm") version "1.5.31"
    id("com.gradle.plugin-publish") version "0.20.0"
    id("io.gitlab.arturbosch.detekt") version "1.20.0-RC1"
    id("net.researchgate.release") version "2.8.1"
    id("com.github.breadmoirai.github-release") version "2.2.12"
    id("com.neva.fork") version "5.0.0"
}

group = "com.cognifide.gradle"
description = "Gradle AEM Plugin"
defaultTasks(":publishToMavenLocal", ":launcher:publishToMavenLocal")

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }

    plugins.withId("java") {
        java {
            withJavadocJar()
            withSourcesJar()
        }
        tasks.withType<JavaCompile>().configureEach{
            sourceCompatibility = JavaVersion.VERSION_1_8.toString()
            targetCompatibility = JavaVersion.VERSION_1_8.toString()
        }
        tasks.withType<Test>().configureEach {
            testLogging.showStandardStreams = true
            useJUnitPlatform()
        }
    }
    plugins.withId("kotlin") {
        tasks.withType<KotlinCompile>().configureEach {
            kotlinOptions {
                jvmTarget = JavaVersion.VERSION_1_8.toString()
            }
        }
    }
}

val functionalTestSourceSet = sourceSets.create("functionalTest")
gradlePlugin.testSourceSets(functionalTestSourceSet)

configurations.getByName("functionalTestImplementation").apply {
    extendsFrom(configurations.getByName("testImplementation"))
}

dependencies {
    // Build environment
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.20.0-RC1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
    implementation("com.cognifide.gradle:common-plugin:1.1.6")

    // External dependencies
    implementation("org.jsoup:jsoup:1.14.3")
    implementation("org.buildobjects:jproc:2.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("commons-io:commons-io:2.11.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.1")
    implementation("org.apache.httpcomponents:httpclient:4.5.13")
    implementation("biz.aQute.bnd:biz.aQute.bnd.gradle:5.3.0")
    implementation("net.lingala.zip4j:zip4j:2.9.1")
    implementation("org.osgi:org.osgi.core:6.0.0")
    implementation("javax.xml.bind:jaxb-api:2.3.1")
    implementation("io.jsonwebtoken:jjwt:0.9.1")
    implementation("org.json:json:20220320")
    implementation("org.bouncycastle:bcpkix-jdk15on:1.70")
}

tasks {
    register<Zip>("assetsZip") {
        from("src/asset")
        archiveFileName.set("assets.zip")
        destinationDirectory.set(file("$buildDir/resources/main"))
    }

    withType<Detekt>().configureEach {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }

    val buildProperties = register("buildProperties") {
        val json = """
        |{
        |    "pluginVersion": "${project.version}",
        |    "gradleVersion": "${project.gradle.gradleVersion}"
        |}""".trimMargin()
        val file = file("$buildDir/resources/main/build.json")

        dependsOn("assetsZip")
        inputs.property("buildJson", json)
        outputs.file(file)
        doLast { file.writeText(json) }
    }

    jar {
        dependsOn(buildProperties)
    }

    publishToMavenLocal {
        dependsOn(jar)
    }

    test {
        dependsOn(buildProperties, "detektTest")
    }

    register<Test>("functionalTest") {
        testClassesDirs = functionalTestSourceSet.output.classesDirs
        classpath = functionalTestSourceSet.runtimeClasspath

        systemProperties(System.getProperties().asSequence().map {
            it.key.toString() to it.value.toString() }.filter {
            it.first.run { startsWith("fileTransfer.") || startsWith("localInstance.") }
        }.toMap())

        useJUnitPlatform()
        mustRunAfter("test")
        dependsOn("jar", "detektFunctionalTest")
        outputs.dir("build/functionalTest")
    }

    afterReleaseBuild {
        dependsOn("publishPlugins")
    }

    named("githubRelease") {
        mustRunAfter("release")
    }

    register("fullRelease") {
        dependsOn("release", "githubRelease")
    }
}

detekt {
    config.from(rootProject.file("detekt.yml"))
    parallel = true
    autoCorrect = true
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
            description = "Provides tasks for working with remote AEM instances."
        }
        create("instance.local") {
            id = "com.cognifide.aem.instance.local"
            implementationClass = "com.cognifide.gradle.aem.instance.LocalInstancePlugin"
            displayName = "AEM Local Instance Plugin"
            description = "Provides tasks for working with local AEM instances."
        }
    }
}

pluginBundle {
    website = "https://github.com/wttech/gradle-aem-plugin"
    vcsUrl = "https://github.com/wttech/gradle-aem-plugin.git"
    description = "Gradle AEM Plugin"
    tags = listOf("aem", "cq", "vault", "scr")
}

githubRelease {
    owner("wttech")
    repo("gradle-aem-plugin")
    token((findProperty("github.token") ?: "").toString())
    tagName(project.version.toString())
    releaseName(project.version.toString())
    draft((findProperty("github.draft") ?: "false").toString().toBoolean())
    overwrite((findProperty("github.override") ?: "true").toString().toBoolean())

    gradle.projectsEvaluated {
        releaseAssets(listOf("jar").map { tasks.named(it) } + project(":launcher").tasks.named("jar"))
    }

    if ((findProperty("github.prerelease") ?: "true").toString().toBoolean()) {
        prerelease(true)
    } else {
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
}

fork {
    properties {
        define("Local instance", mapOf(
                "localInstanceQuickstartJarUri" to {
                    label = "Quickstart URI"
                    description = "For file named 'cq-quickstart-x.x.x.jar'"
                },
                "localInstanceQuickstartLicenseUri" to {
                    label = "Quickstart License URI"
                    description = "For file named 'license.properties'"
                }
        ))
        define("File transfer", mapOf(
                "companyUser" to {
                    label = "User"
                    description = "Authorized to access AEM files"
                    defaultValue = System.getProperty("user.name").orEmpty()
                    optional()
                },
                "companyPassword" to {
                    label = "Password"
                    description = "For above user"
                    optional()
                },
                "companyDomain" to {
                    label = "Domain"
                    description = "Needed only when accessing AEM files over SMB"
                    defaultValue = System.getenv("USERDOMAIN").orEmpty()
                    optional()
                }
        ))
    }
}

