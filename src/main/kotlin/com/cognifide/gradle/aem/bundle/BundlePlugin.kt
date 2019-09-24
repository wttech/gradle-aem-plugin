package com.cognifide.gradle.aem.bundle

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.AemPlugin
import com.cognifide.gradle.aem.bundle.tasks.BundleInstall
import com.cognifide.gradle.aem.bundle.tasks.BundleUninstall
import com.cognifide.gradle.aem.pkg.PackagePlugin
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.compile.JavaCompile

class BundlePlugin : AemPlugin() {

    override fun Project.configure() {
        setupDependentPlugins()
        setupJavaDefaults()
        setupJarTasks()
        setupTasks()
    }

    private fun Project.setupDependentPlugins() {
        plugins.apply(JavaPlugin::class.java)
        plugins.apply(PackagePlugin::class.java)
    }

    private fun Project.setupJavaDefaults() {
        with(convention.getPlugin(JavaPluginConvention::class.java)) {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }

        tasks.withType(JavaCompile::class.java).configureEach { compile ->
            with(compile as JavaCompile) {
                options.encoding = "UTF-8"
                options.compilerArgs = compile.options.compilerArgs + "-Xlint:deprecation"
                options.isIncremental = true
            }
        }
    }

    private fun Project.setupJarTasks() {
        AemExtension.of(this).tasks.jarsAsBundles()
    }

    private fun Project.setupTasks() {
        tasks {
            register<BundleInstall>(BundleInstall.NAME) {
                dependsOn(JavaPlugin.JAR_TASK_NAME)
            }
            register<BundleUninstall>(BundleUninstall.NAME) {
                dependsOn(JavaPlugin.JAR_TASK_NAME)
            }
        }
    }

    companion object {
        const val ID = "com.cognifide.aem.bundle"
    }
}
