package com.cognifide.gradle.aem.launcher

import org.gradle.tooling.GradleConnector
import java.io.File
import java.util.*
import kotlin.system.exitProcess

class Launcher(val args: Array<String>) {

    val printStackTrace get() = args.contains(ARG_PRINT_STACKTRACE)

    val colorOutput get() = !args.contains(ARG_NO_COLOR_OUTPUT)

    val workDirPath get() = args.firstOrNull { it.startsWith("$ARG_WORK_DIR=") }?.substringAfter("=")

    val appDirPath get() = args.firstOrNull { it.startsWith("$ARG_APP_DIR=") }?.substringAfter("=") ?: "maven"

    val gradleArgs get() = args.filterNot { ARGS.contains(it) || ARGS.any { arg -> it.startsWith("$arg=") } }

    val buildConfig get() = Properties().apply {
        load(Launcher::class.java.getResourceAsStream("/build.properties"))
    }
    val gradleVersion get() = buildConfig["gradleVersion"]?.toString()
        ?: throw LauncherException("Gradle version info not available!")

    val pluginVersion get() = buildConfig["pluginVersion"]?.toString()
        ?: throw LauncherException("AEM Plugin version info not available!")

    val currentDir get() = File(".")

    val workDir get() = if (workDirPath != null) currentDir.resolve(workDirPath!!) else currentDir

    val appDir get() = appDirPath.let { workDir.resolve(it) }

    val eol get() = System.lineSeparator()

    val buildScaffolder by lazy { BuildScaffolder(this) }

    val forkScaffolder by lazy { ForkScaffolder(this) }

    val miscScaffolder by lazy { MiscScaffolder(this) }

    fun launch() {
        handleAppDir()
        scaffold()
        awaitFileSystem()
        runBuildWrapperOnce()
        runBuildAndExit()
    }

    private fun handleAppDir() {
        if (!workDir.canonicalPath.contains(appDir.canonicalPath)) {
            println("App dir must be a child dir of work dir!")
            exitProcess(1)
        }

        println("Moving down all files and dirs from work dir '$workDir' to app dir '$appDir'")
        appDir.mkdirs()
        workDir.listFiles()?.forEach { it.renameTo(appDir.resolve(it.name)) }
    }

    private fun scaffold() {
        buildScaffolder.scaffold()
        forkScaffolder.scaffold()
        miscScaffolder.scaffold()
    }

    /**
     * On Windows later build execution might fail without it, because Gradle files might not be ready.
     * Do not remove it!
     */
    @Suppress("MagicNumber")
    private fun awaitFileSystem() {
        Thread.sleep(1_000)
    }

    fun workFileOnce(path: String, action: File.() -> Unit) {
        workDir.resolve(path).apply {
            if (exists()) return@apply
            parentFile.mkdirs()
            action()
        }
    }

    fun workFile(path: String, action: File.() -> Unit) {
        workDir.resolve(path).apply {
            parentFile.mkdirs()
            action()
        }
    }

    fun workFileBackupOnce(path: String, action: File.() -> Unit) {
        workDir.resolve(path).apply {
            if (exists()) {
                renameTo(parentFile.resolve("$name.bak"))
            }
        }
        workFileOnce(path, action)
    }

    fun workFileBackupAndReplaceStrings(path: String, vararg strings: Pair<String, String>) {
        workDir.resolve(path).apply {
            if (exists()) {
                var text = readText()
                for (stringPair in strings) {
                    text = text.replace(stringPair.first, stringPair.second)
                }
                workFileBackupOnce(path) {
                    writeText(text)
                }
            }
        }
    }

    fun runBuildWrapperOnce() = workFile("gradle/wrapper/gradle-wrapper.properties") {
        if (!exists()) {
            println("Generating Gradle wrapper files")
            runBuild(listOf("wrapper", "-Plauncher.wrapper=true"))
        }
    }

    @Suppress("TooGenericExceptionCaught", "PrintStackTrace")
    fun runBuildAndExit(): Unit = try {
        runBuild(gradleArgs)
        exitProcess(0)
    } catch (e: Exception) {
        if (printStackTrace) {
            e.printStackTrace(System.err)
        }
        exitProcess(1)
    }

    private fun runBuild(args: List<String>) {
        println("Running Gradle build: '${args.joinToString(" ")}'")
        GradleConnector.newConnector()
            .useGradleVersion(gradleVersion)
            .forProjectDirectory(workDir)
            .connect().use { connection ->
                connection.newBuild()
                    .withArguments(args)
                    .setColorOutput(colorOutput)
                    .setStandardOutput(System.out)
                    .setStandardError(System.err)
                    .setStandardInput(System.`in`)
                    .run()
            }
    }

    companion object {

        const val ARG_WORK_DIR = "--work-dir"

        const val ARG_APP_DIR = "--app-dir"

        const val ARG_PRINT_STACKTRACE = "--print-stacktrace"

        const val ARG_NO_COLOR_OUTPUT = "--no-color"

        const val ARG_SAVE_PROPS = "--save-props"

        const val ARG_SAVE_PREFIX = "-P"

        val ARGS = listOf(ARG_SAVE_PROPS, ARG_PRINT_STACKTRACE, ARG_NO_COLOR_OUTPUT, ARG_WORK_DIR)

        @JvmStatic
        fun main(args: Array<String>) = Launcher(args).launch()
    }
}
