package com.cognifide.gradle.aem.environment.docker

import com.cognifide.gradle.aem.common.file.FileOperations
import java.io.File
import org.gradle.util.GFileUtils

class ConfigExpander(private val projectDir: String) {

    init {
        setupEnvironmentConfiguration()
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
        private const val ENVIRONMENT_DIR = "environment"
        private const val BUILD_DIR = "build"
        private const val LOGS_DIR = "$BUILD_DIR/logs"
        private const val LOGS_DIR_DISPATCHER = "$LOGS_DIR/dispatcher"
        private const val LOGS_DIR_APACHE = "$LOGS_DIR/apache"
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
            GFileUtils.mkdirs(File("$environmentDir/$LOGS_DIR_DISPATCHER"))
            GFileUtils.mkdirs(File("$environmentDir/$LOGS_DIR_APACHE"))
        }

        fun dispatcherConfPath(projectDir: String) = "${environmentDir(projectDir)}/dispatcher/conf"
    }
}