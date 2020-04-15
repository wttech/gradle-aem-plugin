package com.cognifide.gradle.aem

import com.cognifide.gradle.common.utils.Formats
import java.io.Serializable

class AemPlugin : Serializable {

    lateinit var pluginVersion: String

    lateinit var gradleVersion: String

    companion object {

        const val PKG = "com.cognifide.gradle.aem"

        val BUILD by lazy {
            fromJson(AemPlugin::class.java.getResourceAsStream("/build.json")
                    .bufferedReader().use { it.readText() })
        }

        const val ID = "gradle-aem-plugin"

        const val NAME = "Gradle AEM Plugin"

        val NAME_WITH_VERSION: String get() = "$NAME ${BUILD.pluginVersion}"

        private fun fromJson(json: String): AemPlugin = Formats.toObjectFromJson(json)

        private var once = false

        @Synchronized
        fun once(callback: AemPlugin.() -> Unit) {
            if (!once) {
                callback(BUILD)
                once = true
            }
        }
    }
}
