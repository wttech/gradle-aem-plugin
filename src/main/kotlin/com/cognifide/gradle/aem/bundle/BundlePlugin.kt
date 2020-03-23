package com.cognifide.gradle.aem.bundle

import com.cognifide.gradle.aem.AemException
import com.cognifide.gradle.aem.bundle.tasks.BundleCompose
import com.cognifide.gradle.aem.bundle.tasks.BundleInstall
import com.cognifide.gradle.aem.bundle.tasks.BundleUninstall
import com.cognifide.gradle.aem.common.CommonPlugin
import com.cognifide.gradle.aem.common.tasks.BundleTask
import com.cognifide.gradle.aem.pkg.PackagePlugin
import com.cognifide.gradle.common.CommonDefaultPlugin
import com.cognifide.gradle.common.common
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

class BundlePlugin : CommonDefaultPlugin() {

    override fun Project.configureProject() {
        setupDependentPlugins()
        setupJavaDefaults()
        setupTasks()
        setupTestTask()
    }

    private fun Project.setupDependentPlugins() {
        if (plugins.hasPlugin(PackagePlugin::class.java)) {
            throw AemException("Bundle plugin '$ID' must be applied before package plugin '${PackagePlugin.ID}'!")
        }

        plugins.apply(JavaPlugin::class.java)
        plugins.apply(CommonPlugin::class.java)
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

    private fun Project.setupTasks() = tasks {
        val compose = register<BundleCompose>(BundleCompose.NAME) {
            dependsOn(JavaPlugin.CLASSES_TASK_NAME)
        }.apply {
            artifacts.add(Dependency.ARCHIVES_CONFIGURATION, this)
            artifacts.add(JavaPlugin.API_ELEMENTS_CONFIGURATION_NAME, this)
            artifacts.add(JavaPlugin.RUNTIME_ELEMENTS_CONFIGURATION_NAME, this)
            artifacts.add(JavaPlugin.TEST_RUNTIME_CLASSPATH_CONFIGURATION_NAME, this)
        }
        register<BundleInstall>(BundleInstall.NAME) {
            dependsOn(compose)
        }
        register<BundleUninstall>(BundleUninstall.NAME) {
            dependsOn(compose)
        }
        typed<BundleTask> {
            files.from(compose.map { it.archiveFile })
        }
        named<Task>(LifecycleBasePlugin.ASSEMBLE_TASK_NAME) {
            dependsOn(compose)
        }
        named<Jar>(JavaPlugin.JAR_TASK_NAME) {
            archiveClassifier.set(LIB_CLASSIFIER)
        }
    }

    /**
     * @see <https://github.com/Cognifide/gradle-aem-plugin/issues/95>
     */
    private fun Project.setupTestTask() = afterEvaluate {
        tasks {
            named<Test>(JavaPlugin.TEST_TASK_NAME) {
                val testImplConfig = configurations.getByName(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME)
                val compileOnlyConfig = configurations.getByName(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME)

                testImplConfig.extendsFrom(compileOnlyConfig)

                val bundle = common.tasks.get<BundleCompose>(BundleCompose.NAME)
                dependsOn(bundle)
                classpath += files(bundle.composedFile)
            }
        }
    }

    companion object {
        const val ID = "com.cognifide.aem.bundle"

        const val LIB_CLASSIFIER = "lib"
    }
}
