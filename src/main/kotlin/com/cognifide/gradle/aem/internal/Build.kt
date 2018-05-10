package com.cognifide.gradle.aem.internal

import com.fasterxml.jackson.databind.ObjectMapper

class Build private constructor() {

    class Metadata {

        lateinit var version: String

    }

    companion object {

        private val METADATA by lazy {
            fromJson(Build::class.java.getResourceAsStream("/build.json")
                    .bufferedReader().use { it.readText() })
        }

        private fun fromJson(json: String): Metadata {
            return ObjectMapper().readValue(json, Metadata::class.java)
        }

        private var greeted = false

        @Synchronized
        fun greetOnce(callback: (Metadata) -> Unit) {
            if (!greeted) {
                callback(METADATA)
                greeted = true
            }
        }

    }

}