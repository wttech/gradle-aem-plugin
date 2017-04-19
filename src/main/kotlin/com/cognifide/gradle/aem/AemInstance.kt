package com.cognifide.gradle.aem

import java.io.Serializable

data class AemInstance(
        val url: String,
        val user: String,
        val password: String,
        val group: String
) : Serializable