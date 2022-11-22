package com.cognifide.gradle.aem.launcher

import java.util.Properties

class BuildScaffolder(private val launcher: Launcher) {

    fun scaffold() {
        saveProperties()
        saveSettings()
        when (aemVersion) {
            "cloud" -> EnvCloudScaffolder(launcher).scaffold()
            null -> EnvInstanceOnlyScaffolder(launcher).scaffold()
            else -> EnvOnPremScaffolder(launcher).scaffold()
        }
    }

    private val aemVersion get() = archetypeProperties?.getProperty("aemVersion")

    private val archetypeProperties get() = if (archetypePropertiesFile.exists()) Properties().apply {
        archetypePropertiesFile.inputStream().buffered().use { load(it) }
    } else null

    private val archetypePropertiesFile get() = launcher.appDir.resolve("archetype.properties")

    private fun saveProperties() = launcher.workFileOnce("gradle.properties") {
        println("Saving Gradle properties file '$this'")
        outputStream().use { output ->
            Properties().apply {
                if (savePropsFlag) {
                    putAll(saveProps)
                }
                store(output, null)
            }
        }
    }

    private val savePropsFlag get() = launcher.args.contains(Launcher.ARG_SAVE_PROPS)

    private val saveProps get() = launcher.args.filter { it.startsWith(Launcher.ARG_SAVE_PREFIX) }
        .map { it.removePrefix(Launcher.ARG_SAVE_PREFIX) }
        .associate { it.substringBefore("=") to it.substringAfter("=") }

    private fun saveSettings() = launcher.workFileOnce("settings.gradle.kts") {
        println("Saving Gradle settings file '$this'")
        writeText(
            """
            include(":env")
            """.trimIndent()
        )
    }
}
