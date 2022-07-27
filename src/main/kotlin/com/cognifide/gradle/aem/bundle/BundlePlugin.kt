package com.cognifide.gradle.aem.bundle

import com.cognifide.gradle.aem.AemException
import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.bundle.tasks.BundleInstall
import com.cognifide.gradle.aem.bundle.tasks.BundleJar
import com.cognifide.gradle.aem.bundle.tasks.BundleUninstall
import com.cognifide.gradle.aem.common.CommonPlugin
import com.cognifide.gradle.aem.common.tasks.Bundle
import com.cognifide.gradle.aem.pkg.PackagePlugin
import com.cognifide.gradle.common.CommonDefaultPlugin
import com.cognifide.gradle.common.CommonExtension
import com.cognifide.gradle.common.common
import com.cognifide.gradle.common.tasks.configureApply
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test

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
        val support by lazy { extensions.getByType(CommonExtension::class.java).javaSupport }

        with(extensions.getByType(JavaPluginExtension::class.java)) {
            sourceCompatibility = support.compatibilityVersion.get()
            targetCompatibility = support.compatibilityVersion.get()
        }

        tasks {
            typed<JavaCompile> {
                javaCompiler.set(support.compiler)
                options.apply {
                    encoding = "UTF-8"
                    compilerArgs = compilerArgs + "-Xlint:deprecation"
                    isIncremental = true
                }
            }
            typed<Test> {
                javaLauncher.set(support.launcher)
            }
        }
    }

    private fun Project.setupTasks() {
        tasks {
            val jar = named<Jar>(JavaPlugin.JAR_TASK_NAME) {
                val bundle = BundleJar(this).also { AemExtension.bundleJarMap[this] = it }
                bundle.applyDefaults()
                doLast(object : Action<Task> { // https://docs.gradle.org/7.4.1/userguide/validation_problems.html#implementation_unknown
                    override fun execute(task: Task) {
                        bundle.runBndTool()
                    }
                })
            }.apply {
                afterEvaluate { configureApply { AemExtension.bundleJarMap[this]?.applyEvaluated() } }
            }
            register<BundleInstall>(BundleInstall.NAME) {
                dependsOn(jar)
            }
            register<BundleUninstall>(BundleUninstall.NAME) {
                dependsOn(jar)
            }
            typed<Bundle> {
                files(jar)
            }
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

                val bundle = common.tasks.get<Jar>(JavaPlugin.JAR_TASK_NAME)
                dependsOn(bundle)
                classpath += files(bundle)
            }
        }
    }

    companion object {
        const val ID = "com.cognifide.aem.bundle"
    }
}
