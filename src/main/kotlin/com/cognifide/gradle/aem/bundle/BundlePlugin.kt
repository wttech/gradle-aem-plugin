package com.cognifide.gradle.aem.bundle

import com.cognifide.gradle.aem.AemPlugin
import com.cognifide.gradle.aem.bundle.tasks.BundleCompose
import com.cognifide.gradle.aem.bundle.tasks.BundleInstall
import com.cognifide.gradle.aem.bundle.tasks.BundleUninstall
import com.cognifide.gradle.aem.pkg.PackagePlugin
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Dependency
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.language.base.plugins.LifecycleBasePlugin

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
                dependsOn(JavaPlugin.CLASSES_TASK_NAME)
            }.apply {
                artifacts.add(Dependency.ARCHIVES_CONFIGURATION, this)
                artifacts.add(JavaPlugin.API_ELEMENTS_CONFIGURATION_NAME, this)
                artifacts.add(JavaPlugin.RUNTIME_ELEMENTS_CONFIGURATION_NAME, this)
                artifacts.add(JavaPlugin.TEST_RUNTIME_CLASSPATH_CONFIGURATION_NAME, this)
            }
            register<BundleInstall>(BundleInstall.NAME) {
                dependsOn(BundleCompose.NAME)
            }
            register<BundleUninstall>(BundleUninstall.NAME) {
                dependsOn(BundleCompose.NAME)
            }
            named<Task>(LifecycleBasePlugin.ASSEMBLE_TASK_NAME) {
                dependsOn(BundleCompose.NAME)
            }
            named<Jar>(JavaPlugin.JAR_TASK_NAME) {
                archiveClassifier.set(LIB_CLASSIFIER)
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

                    bundles.forEach { bundle ->
                        dependsOn(bundle)
                        classpath += files(bundle.composedFile)
                    }
                }
            }
        }
    }

    companion object {
        const val ID = "com.cognifide.aem.bundle"

        const val LIB_CLASSIFIER = "lib"
    }
}
