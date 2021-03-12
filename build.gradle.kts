import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import io.gitlab.arturbosch.detekt.Detekt

plugins {
    id("java-gradle-plugin")
    id("maven-publish")
    id("org.jetbrains.kotlin.jvm") version "1.4.20"
    id("org.jetbrains.dokka") version "1.4.0-rc"
    id("com.gradle.plugin-publish") version "0.11.0"
    id("io.gitlab.arturbosch.detekt") version "1.7.0"
    id("com.jfrog.bintray") version "1.8.4"
    id("net.researchgate.release") version "2.8.1"
    id("com.github.breadmoirai.github-release") version "2.2.10"
    id("com.neva.fork") version "5.0.0"
}

group = "com.cognifide.gradle"
description = "Gradle AEM Plugin"
defaultTasks(":publishToMavenLocal", ":launcher:publishToMavenLocal")

val functionalTestSourceSet = sourceSets.create("functionalTest")
gradlePlugin.testSourceSets(functionalTestSourceSet)

configurations.getByName("functionalTestImplementation").apply {
    extendsFrom(configurations.getByName("testImplementation"))
}

repositories {
    mavenLocal()
    jcenter()
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.4.20")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.4.20")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9")

    implementation("com.cognifide.gradle:common-plugin:1.0.24")

    implementation("org.jsoup:jsoup:1.12.1")
    implementation("org.buildobjects:jproc:2.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9")
    implementation("org.apache.commons:commons-lang3:3.9")
    implementation("commons-io:commons-io:2.6")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.10.3")
    implementation("org.apache.httpcomponents:httpclient:4.5.12")
    implementation("biz.aQute.bnd:biz.aQute.bnd.gradle:5.2.0")
    implementation("net.lingala.zip4j:zip4j:2.5.1")
    implementation("org.osgi:org.osgi.core:6.0.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.5.2")

    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.7.0")
}

tasks {

    register<Zip>("assetsZip") {
        from("src/asset")
        archiveFileName.set("assets.zip")
        destinationDirectory.set(file("$buildDir/resources/main"))
    }
    register<Jar>("sourcesJar") {
        archiveClassifier.set("sources")
        dependsOn("classes")
        from(sourceSets["main"].allSource)
    }

    dokkaJavadoc {
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

    withType<Detekt>().configureEach {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }

    build {
        dependsOn("sourcesJar", "javadocJar")
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

    withType<Test>().configureEach {
        testLogging.showStandardStreams = true
        useJUnitPlatform()
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
        dependsOn("bintrayUpload", "publishPlugins")
    }

    named("githubRelease") {
        mustRunAfter("release")
    }

    register("fullRelease") {
        dependsOn("release", "githubRelease")
    }
}

detekt {
    config.from(file("detekt.yml"))
    parallel = true
    autoCorrect = true
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

bintray {
    user = (findProperty("bintray.user") ?: System.getenv("BINTRAY_USER"))?.toString()
    key = (findProperty("bintray.key") ?: System.getenv("BINTRAY_KEY"))?.toString()
    setPublications("mavenJava")
    with(pkg) {
        repo = "maven-public"
        name = "gradle-aem-plugin"
        userOrg = "wttech"
        setLicenses("Apache-2.0")
        vcsUrl = "https://github.com/wttech/gradle-aem-plugin.git"
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
    owner("wttech")
    repo("gradle-aem-plugin")
    token((findProperty("github.token") ?: "").toString())
    tagName(project.version.toString())
    releaseName(project.version.toString())
    draft((findProperty("github.draft") ?: "false").toString().toBoolean())
    overwrite((findProperty("github.override") ?: "true").toString().toBoolean())

    gradle.projectsEvaluated {
        releaseAssets(listOf("jar", "sourcesJar", "javadocJar").map { tasks.named(it) }
                + project(":launcher").tasks.named("jar"))
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

