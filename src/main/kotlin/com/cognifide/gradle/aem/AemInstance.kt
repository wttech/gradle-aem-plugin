package com.cognifide.gradle.aem

import com.cognifide.gradle.aem.internal.Formats
import com.cognifide.gradle.aem.internal.PropertyParser
import org.gradle.api.Project
import java.io.Serializable

/**
 * TODO inherit from this, introduce methods 'config.localInstance()', 'config.remoteInstance() = config.instance()'
 */
data class AemInstance(
        val url: String,
        val user: String,
        val password: String,
        val name: String,
        val debugPort: String? = null
) : Serializable {

    companion object {

        val FILTER_ANY = PropertyParser.FILTER_DEFAULT

        val FILTER_LOCAL = "local-*"

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

        fun filter(project: Project, instanceFilter: String = FILTER_LOCAL): List<AemInstance> {
            val config = AemConfig.of(project)
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
                PropertyParser(project).filter(instance.name, "aem.deploy.instance.name", instanceFilter)
            }
        }
    }

    val credentials: String
        get() = "$user:$password"

    fun validate() {
        if (!Formats.URL_VALIDATOR.isValid(url)) {
            throw AemException("Malformed URL address detected in $this")
        }

        if (user.isBlank()) {
            throw AemException("User cannot be blank in $this")
        }

        if (password.isBlank()) {
            throw AemException("Password cannot be blank in $this")
        }

        if (name.isBlank()) {
            throw AemException("Name cannot be blank in $this")
        }
    }

}