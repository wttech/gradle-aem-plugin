package com.cognifide.gradle.sling.bundle

import com.cognifide.gradle.sling.SlingException
import com.cognifide.gradle.sling.bundle.tasks.BundleJar
import com.cognifide.gradle.sling.bundle.tasks.BundleInstall
import com.cognifide.gradle.sling.bundle.tasks.BundleUninstall
import com.cognifide.gradle.sling.common.CommonPlugin
import com.cognifide.gradle.sling.common.tasks.BundleTask
import com.cognifide.gradle.sling.pkg.PackagePlugin
import com.cognifide.gradle.common.CommonDefaultPlugin
import com.cognifide.gradle.common.common
import com.cognifide.gradle.common.tasks.configureApply
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
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
            throw SlingException("Bundle plugin '$ID' must be applied before package plugin '${PackagePlugin.ID}'!")
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

    private fun Project.setupTasks() {
        tasks {
            val jar = named<Jar>(JavaPlugin.JAR_TASK_NAME) {
                val bundle = BundleJar(this).also { convention.plugins[CONVENTION_PLUGIN] = it }
                bundle.applyDefaults()
                doLast { bundle.runBndTool() }
            }.apply {
                afterEvaluate { configureApply { convention.getPlugin(BundleJar::class.java).applyEvaluated() } }
            }
            register<BundleInstall>(BundleInstall.NAME) {
                dependsOn(jar)
            }
            register<BundleUninstall>(BundleUninstall.NAME) {
                dependsOn(jar)
            }
            typed<BundleTask> {
                files.from(jar)
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
        const val ID = "com.cognifide.sling.bundle"

        const val CONVENTION_PLUGIN = "bundle"
    }
}
