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
        val environment: String,
        val type: String,
        val debugPort: String? = null,
        val httpPort: String? = null
) : Serializable {

    companion object {

        val FILTER_ANY = PropertyParser.FILTER_DEFAULT

        val FILTER_LOCAL = "local-*"

        val FILTER_AUTHOR = "*-author"

        val ENVIRONMENT_CMD = "cmd"

        val ENVIRONMENT_LOCAL = "local"

        val TYPE_AUTHOR = "author"

        val TYPE_PUBLISH = "publish"

        fun parse(str: String): List<AemInstance> {
            return str.split(";").map { line ->
                // TODO auto-detect type basing on port number when 3 parts specified not 4/ maybe using constructor
                val (url, type, user, password) = line.split(",")

                AemInstance(url, user, password, ENVIRONMENT_CMD, type)
            }
        }

        fun defaults(): List<AemInstance> {
            return listOf(
                    AemInstance("http://localhost:4502", "admin", "admin", ENVIRONMENT_LOCAL, TYPE_AUTHOR),
                    AemInstance("http://localhost:4503", "admin", "admin", ENVIRONMENT_LOCAL, TYPE_PUBLISH)
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

    val name: String
        get() = "$environment-$type"

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

        if (environment.isBlank()) {
            throw AemException("Environment cannot be blank in $this")
        }

        if (type.isBlank()) {
            throw AemException("Type cannot be blank in $this")
        }
    }

}