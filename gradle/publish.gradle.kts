import com.jfrog.bintray.gradle.BintrayExtension

configure<GradlePluginDevelopmentExtension> {
    plugins {
        create("config") {
            id = "com.cognifide.aem.config"
            implementationClass = "com.cognifide.gradle.aem.config.ConfigPlugin"
        }
        create("tooling") {
            id = "com.cognifide.aem.tooling"
            implementationClass = "com.cognifide.gradle.aem.tooling.ToolingPlugin"
        }
        create("package") {
            id = "com.cognifide.aem.package"
            implementationClass = "com.cognifide.gradle.aem.pkg.PackagePlugin"
        }
        create("bundle") {
            id = "com.cognifide.aem.bundle"
            implementationClass = "com.cognifide.gradle.aem.bundle.BundlePlugin"
        }
        create("instance") {
            id = "com.cognifide.aem.instance"
            implementationClass = "com.cognifide.gradle.aem.instance.InstancePlugin"
        }
    }
}

configure<PublishingExtension> {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(tasks["sourcesJar"])
        }
    }
}

configure<BintrayExtension> {
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