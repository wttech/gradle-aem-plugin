package com.cognifide.gradle.aem.launcher

import org.gradle.tooling.GradleConnector
import java.io.File
import java.util.*
import kotlin.system.exitProcess

class Launcher(val args: Array<String>) {

    val printStackTrace get() = args.contains(ARG_PRINT_STACKTRACE)

    val colorOutput get() = !args.contains(ARG_NO_COLOR_OUTPUT)

    val workDirPath get() = args.firstOrNull { it.startsWith("$ARG_WORK_DIR=") }?.substringAfter("=")

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

    val eol get() = System.lineSeparator()

    val buildScaffolder by lazy { BuildScaffolder(this) }

    val forkScaffolder by lazy { ForkScaffolder(this) }

    val miscScaffolder by lazy { MiscScaffolder(this) }

    fun launch() {
        scaffold()
        ensureWrapper()
        runBuildAndExit()
    }

    private fun scaffold() {
        buildScaffolder.scaffold()
        forkScaffolder.scaffold()
        miscScaffolder.scaffold()
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

    fun ensureWrapper() = workFile("gradle-wrapper.properties") {
        if (!exists()) {
            println("Generating Gradle wrapper files")
            runBuild(listOf("wrapper", "-Plauncher.wrapper=true"))
        }
    }

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

        val ARG_WORK_DIR = "--work-dir"

        val ARG_PRINT_STACKTRACE = "--print-stacktrace"

        val ARG_NO_COLOR_OUTPUT = "--no-color"

        val ARG_SAVE_PROPS = "--save-props"

        val ARG_SAVE_PREFIX = "-P"

        val ARGS = listOf(ARG_SAVE_PROPS, ARG_PRINT_STACKTRACE, ARG_NO_COLOR_OUTPUT, ARG_WORK_DIR)

        @JvmStatic
        fun main(args: Array<String>) = Launcher(args).launch()
    }
}

