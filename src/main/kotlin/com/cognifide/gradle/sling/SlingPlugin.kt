package com.cognifide.gradle.sling

import com.cognifide.gradle.common.utils.Formats
import java.io.Serializable

class SlingPlugin : Serializable {

    lateinit var pluginVersion: String

    lateinit var gradleVersion: String

    companion object {

        val BUILD by lazy {
            fromJson(SlingPlugin::class.java.getResourceAsStream("/build.json")
                    .bufferedReader().use { it.readText() })
        }

        const val ID = "gradle-sling-plugin"

        const val NAME = "Gradle Sling Plugin"

        val NAME_WITH_VERSION: String get() = "$NAME ${BUILD.pluginVersion}"

        private fun fromJson(json: String): SlingPlugin = Formats.toObjectFromJson(json)

        private var once = false

        @Synchronized
        fun once(callback: SlingPlugin.() -> Unit) {
            if (!once) {
                callback(BUILD)
                once = true
            }
        }
    }
}
