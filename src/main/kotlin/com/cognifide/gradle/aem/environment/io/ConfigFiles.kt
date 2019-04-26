package com.cognifide.gradle.aem.environment.io

import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.common.AemTask
import com.cognifide.gradle.aem.common.Patterns
import com.cognifide.gradle.aem.common.file.FileOperations
import com.cognifide.gradle.aem.common.file.resolver.FileResolver
import com.cognifide.gradle.aem.environment.EnvironmentException
import java.io.File
import org.gradle.util.GFileUtils

class ConfigFiles(private val aem: AemExtension) {

    private val options = aem.config.environmentOptions

    private val downloadDir = AemTask.temporaryDir(aem.project, TEMPORARY_DIR)

    private val fileResolver = FileResolver(aem, downloadDir).apply { group(GROUP_EXTRA) {} }

    val dockerComposeFile
        get() = File("$rootDir/docker-compose.yml")

    val dispatcherConfDir
        get() = File("$rootDir/$DISPATCHER_DIR/conf")

    val dispatcherModuleFile: File
        get() = File("${options.root}/$DISPATCHER_DIST_DIR/mod_dispatcher.so")

    val rootDir: File
        get() = File(options.root)

    private val dispatcherDistUrl: String
        get() = options.dispatcherDistUrl.ifBlank { null }
                ?: throw EnvironmentException("Dispatcher distribution URL needs to be configured in property" +
                        " 'aem.env.dispatcher.distUrl' in order to use AEM environment.")

    fun prepare() {
        if (!rootDir.exists()) {
            FileOperations.copyResources(ENVIRONMENT_DIR, rootDir)
        }

        if (!dispatcherModuleFile.exists()) {
            GFileUtils.copyFile(downloadDispatcherModule(), dispatcherModuleFile)
        }

        ensureDirs()
    }

    private fun downloadDispatcherModule(): File {
        val tarFile = fileResolver.url(dispatcherDistUrl).file
        val tarTree = aem.project.tarTree(tarFile)

        return tarTree.find { Patterns.wildcard(it, options.dispatcherModuleName) }
                ?: throw EnvironmentException("Dispatcher distribution seems to be invalid." +
                        " Cannot find file matching '${options.dispatcherModuleName}' in '$tarFile'")
    }

    private fun ensureDirs() {
        GFileUtils.mkdirs(File("$rootDir/$CACHE_DIR_DEMO"))
        GFileUtils.mkdirs(File("$rootDir/$CACHE_DIR_LIVE"))
        GFileUtils.mkdirs(File("$rootDir/$LOGS_DIR"))
    }

    companion object {

        const val GROUP_EXTRA = "extra"

        const val TEMPORARY_DIR = "environmentDistributions"

        const val ENVIRONMENT_DIR = "environment"

        const val BUILD_DIR = "build"

        const val LOGS_DIR = "$BUILD_DIR/logs"

        const val DISPATCHER_DIR = "dispatcher"

        const val DISPATCHER_DIST_DIR = "$BUILD_DIR/distributions"

        const val CACHE_DIR_LIVE = "$BUILD_DIR/cache/content/example/live"

        const val CACHE_DIR_DEMO = "$BUILD_DIR/cache/content/example/demo"
    }
}