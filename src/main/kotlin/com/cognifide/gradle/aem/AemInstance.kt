package com.cognifide.gradle.aem

import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOCase
import org.gradle.api.Project
import java.io.Serializable

data class AemInstance(
        val url: String,
        val user: String,
        val password: String,
        val group: String
) : Serializable {

    companion object {

        val FILTER_DEFAULT = "*"

        val FILTER_AUTHOR = "*-author"

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

        fun filter(project: Project, config: AemConfig, instanceGroup: String = AemInstance.FILTER_DEFAULT): List<AemInstance> {
            val instanceValues = project.properties["aem.deploy.instance.list"] as String?
            if (!instanceValues.isNullOrBlank()) {
                return AemInstance.parse(instanceValues!!)
            }

            val instances = if (config.instances.isEmpty()) {
                return AemInstance.defaults()
            } else {
                config.instances
            }

            return instances.filter { instance ->
                val group = project.properties.getOrElse("aem.deploy.instance.group", { instanceGroup }) as String

                FilenameUtils.wildcardMatch(instance.group, group, IOCase.INSENSITIVE)
            }
        }

    }

}