package com.cognifide.gradle.aem

import java.io.Serializable

data class AemInstance(
        val url: String,
        val user: String,
        val password: String,
        val group: String
) : Serializable {

    companion object {
        fun fromString(values: String?): List<AemInstance> {
            val instances = mutableListOf<AemInstance>()

            // TODO parse
            // http://localhost:4502,admin,admin,local-author;http://localhost:4503,admin,admin,local-publish

            return instances
        }
    }

}