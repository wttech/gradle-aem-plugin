package com.cognifide.gradle.aem

import java.io.Serializable

data class AemInstance(
        val url: String,
        val user: String,
        val password: String,
        val group: String
) : Serializable {

    companion object {

        fun parse(str: String): List<AemInstance> {
            return str.split(";").map { line ->
                val (url, user, password) = line.split(",")

                AemInstance(url, user, password, "command-line")
            }
        }

        fun defaults(): List<AemInstance> {
            return listOf(
                    AemInstance("http://localhost:4502", "admin", "admin", "local-author"),
                    AemInstance("http://localhost:4503", "admin", "admin", "local-publish")
            )
        }

    }

}