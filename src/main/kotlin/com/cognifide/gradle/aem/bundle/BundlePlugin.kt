package com.cognifide.gradle.aem.bundle

import com.cognifide.gradle.aem.AemPlugin
import com.cognifide.gradle.aem.bundle.tasks.BundleCompose
import com.cognifide.gradle.aem.bundle.tasks.BundleInstall
import com.cognifide.gradle.aem.bundle.tasks.BundleUninstall
import com.cognifide.gradle.aem.pkg.PackagePlugin
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test

class BundlePlugin : AemPlugin() {

    override fun Project.configure() {
        setupDependentPlugins()
        setupJavaDefaults()
        setupTasks()
        setupTestTask()
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

        tasks {
            named<Jar>(JavaPlugin.JAR_TASK_NAME) {
                archiveBaseName.set(aem.baseName)
            }

            typed<JavaCompile> {
                options.encoding = "UTF-8"
                options.compilerArgs = options.compilerArgs + "-Xlint:deprecation"
                options.isIncremental = true
            }
        }
    }

    private fun Project.setupTasks() {
        tasks {
            register<BundleCompose>(BundleCompose.NAME) {
                dependsOn(JavaPlugin.JAR_TASK_NAME)
            }
            register<BundleInstall>(BundleInstall.NAME) {
                dependsOn(JavaPlugin.JAR_TASK_NAME)
            }
            register<BundleUninstall>(BundleUninstall.NAME) {
                dependsOn(JavaPlugin.JAR_TASK_NAME)
            }
        }
    }

    // @see <https://github.com/Cognifide/gradle-aem-plugin/issues/95>
    private fun Project.setupTestTask() {
        afterEvaluate {
            tasks {
                named<Test>(JavaPlugin.TEST_TASK_NAME) {
                    val testImplConfig = configurations.getByName(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME)
                    val compileOnlyConfig = configurations.getByName(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME)

                    testImplConfig.extendsFrom(compileOnlyConfig)

                    bundles.forEach { jar ->
                        dependsOn(jar)
                        classpath += files(jar.archiveFile.get().asFile)
                    }
                }
            }
        }
    }

    companion object {
        const val ID = "com.cognifide.aem.bundle"
    }
}
