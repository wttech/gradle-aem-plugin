package com.cognifide.gradle.aem.api

import com.fasterxml.jackson.databind.ObjectMapper

class AemPlugin private constructor() {

    class Build {

        lateinit var version: String

    }

    companion object {

        val BUILD by lazy {
            fromJson(AemPlugin::class.java.getResourceAsStream("/build.json")
                    .bufferedReader().use { it.readText() })
        }

        val NAME = "Gradle AEM Plugin"

        val NAME_WITH_VERSION : String
            get() = "$NAME ${BUILD.version}"

        private fun fromJson(json: String): Build {
            return ObjectMapper().readValue(json, Build::class.java)
        }

        private var once = false

        @Synchronized
        fun once(callback: (Build) -> Unit) {
            if (!once) {
                callback(BUILD)
                once = true
            }
        }

    }

}