package com.cognifide.gradle.aem

import com.cognifide.gradle.aem.internal.PropertyParser
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOCase
import org.apache.commons.validator.routines.UrlValidator
import org.gradle.api.Project
import java.io.Serializable

data class AemInstance(
        val url: String,
        val user: String,
        val password: String,
        val group: String
) : Serializable {

    companion object {

        val FILTER_DEFAULT = PropertyParser.FILTER_DEFAULT

        val FILTER_AUTHOR = "*-author"

        val URL_VALIDATOR = UrlValidator(arrayOf("http", "https"), UrlValidator.ALLOW_LOCAL_URLS)

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

        fun filter(project: Project, config: AemConfig, instanceGroup: String = FILTER_DEFAULT): List<AemInstance> {
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
                PropertyParser(project).filter(instance.group, "aem.deploy.instance.group", instanceGroup)
            }
        }
    }

    fun validate() {
        if (!URL_VALIDATOR.isValid(url)) {
            throw AemException("Malformed URL address detected in $this")
        }

        if (user.isBlank()) {
            throw AemException("User cannot be blank in $this")
        }

        if (password.isBlank()) {
            throw AemException("Password cannot be blank in $this")
        }

        if (group.isBlank()) {
            throw AemException("Group cannot be blank in $this")
        }
    }

}