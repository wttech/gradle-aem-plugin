import com.gradle.publish.PluginBundleExtension
import com.jfrog.bintray.gradle.BintrayExtension

configure<PublishingExtension> {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(tasks["sourcesJar"])
            artifact(tasks["javadocJar"])
        }
    }
}

configure<GradlePluginDevelopmentExtension> {
    plugins {
        create("common") {
            id = "com.cognifide.aem.common"
            implementationClass = "com.cognifide.gradle.aem.common.CommonPlugin"
            displayName = "AEM Common Plugin"
            description = "Provides AEM DSL / 'aem' extension to build script on which all other logic is based."
        }
        create("tooling") {
            id = "com.cognifide.aem.tooling"
            implementationClass = "com.cognifide.gradle.aem.tooling.ToolingPlugin"
            displayName = "AEM Tooling Plugin"
            description = "Provides tasks like 'rcp', 'sync', 'vlt' for working with content using JCR File Vault."
        }
        create("package") {
            id = "com.cognifide.aem.package"
            implementationClass = "com.cognifide.gradle.aem.pkg.PackagePlugin"
            displayName = "AEM Package Plugin"
            description = "Provides tasks for working with CRX packages."
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

configure<PluginBundleExtension> {
    website = "https://github.com/Cognifide/gradle-aem-plugin"
    vcsUrl = "https://github.com/Cognifide/gradle-aem-plugin.git"
    description = "Gradle AEM Plugin"
    tags = listOf("aem", "cq", "vault", "scr")
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
