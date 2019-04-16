package com.cognifide.gradle.aem.environment.io

import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.common.AemTask
import com.cognifide.gradle.aem.common.file.FileOperations
import com.cognifide.gradle.aem.common.file.resolver.FileResolver
import com.cognifide.gradle.aem.environment.EnvironmentException
import java.io.File
import org.gradle.util.GFileUtils

class ConfigExpander(private val aem: AemExtension) {

    private val projectDir: String = aem.project.projectDir.path

    private val downloadDir = AemTask.temporaryDir(aem.project, TEMPORARY_DIR)

    private val fileResolver = FileResolver(aem, downloadDir).apply { group(GROUP_EXTRA) {} }

    private val dispatcherDistUrl = aem.environmentOptions.dispatcherDistUrl

    init {
        setupEnvironmentConfiguration()
        setupDispatcherDistribution()
    }

    private fun setupDispatcherDistribution() {
        val dispatcherModuleFile = dispatcherModuleFile(projectDir)
        if (dispatcherModuleFile.exists()) {
            return
        }
        if (dispatcherDistUrl.isBlank()) {
            throw EnvironmentException("Dispatcher distribution URL needs to be configured in order to turn on the environment:" +
                    "\naem.environment.dispatcher.apache24-linux-x64.distUrl")
        }

        val dispatcherDist = fileResolver.run { dispatcherDistUrl.run { url(this) } }.file
        val dispatcherDistTree = aem.project.tarTree(dispatcherDist)
        val sourceDispatcherModuleFile = dispatcherDistTree.find { file -> file.name.startsWith("dispatcher-apache2.4") and file.name.endsWith(".so") }
                ?: throw EnvironmentException("Dispatcher distribution seems to be invalid. Cannot find 'dispatcher-apache2.4*.so' file in '$dispatcherDist'")
        GFileUtils.copyFile(sourceDispatcherModuleFile, dispatcherModuleFile)
    }

    val composeFilePath: String = composeFile(projectDir).path
    val dispatcherConfPath: String = dispatcherConfPath(projectDir)

    private fun setupEnvironmentConfiguration() {
        if (isEnvironmentConfigExpanded(projectDir)) {
            return
        }
        copyEnvironmentFilesFromPlugin()
        createDispatcherBuildDirs(projectDir)
    }

    private fun copyEnvironmentFilesFromPlugin() {
        FileOperations.copyResources(ENVIRONMENT_DIR, File(environmentDir(projectDir)), skipExisting = true)
    }

    companion object {
        private const val GROUP_EXTRA = "extra"
        private const val TEMPORARY_DIR = "environmentDistributions"
        private const val ENVIRONMENT_DIR = "environment"
        private const val BUILD_DIR = "build"
        private const val LOGS_DIR = "$BUILD_DIR/logs"
        private const val DISPATCHER_DIR = "dispatcher"
        private const val DISPATCHER_DIST_DIR = "$BUILD_DIR/distributions"
        private const val CACHE_DIR_LIVE = "$BUILD_DIR/cache/content/example/live"
        private const val CACHE_DIR_DEMO = "$BUILD_DIR/cache/content/example/demo"
        private const val SINGLE_MODULE_PROJECT_AEM_GRADLE_DIR = "gradle"
        private const val MULTI_MODULE_PROJECT_AEM_GRADLE_DIR = "aem/gradle"

        private fun environmentDir(projectDir: String) =
                if (File("$projectDir/$MULTI_MODULE_PROJECT_AEM_GRADLE_DIR").exists()) {
                    "$projectDir/$MULTI_MODULE_PROJECT_AEM_GRADLE_DIR/$ENVIRONMENT_DIR"
                } else {
                    "$projectDir/$SINGLE_MODULE_PROJECT_AEM_GRADLE_DIR/$ENVIRONMENT_DIR"
                }

        private fun isEnvironmentConfigExpanded(projectDir: String) =
                File(environmentDir(projectDir)).exists()

        private fun composeFile(projectDir: String) = File("${environmentDir(projectDir)}/docker-compose.yml")
        private fun createDispatcherBuildDirs(projectDir: String) {
            val environmentDir = environmentDir(projectDir)
            GFileUtils.mkdirs(File("$environmentDir/$CACHE_DIR_DEMO"))
            GFileUtils.mkdirs(File("$environmentDir/$CACHE_DIR_LIVE"))
            GFileUtils.mkdirs(File("$environmentDir/$LOGS_DIR"))
            GFileUtils.mkdirs(File("$environmentDir/$LOGS_DIR"))
        }

        private fun dispatcherConfPath(projectDir: String) = "${environmentDir(projectDir)}/$DISPATCHER_DIR/conf"
        private fun dispatcherModuleFile(projectDir: String) = File("${environmentDir(projectDir)}/$DISPATCHER_DIST_DIR/mod_dispatcher.so")
    }
}