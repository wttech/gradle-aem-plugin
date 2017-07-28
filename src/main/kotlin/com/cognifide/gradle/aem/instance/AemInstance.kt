package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.AemConfig
import com.cognifide.gradle.aem.AemException
import com.cognifide.gradle.aem.internal.Formats
import com.cognifide.gradle.aem.internal.PropertyParser
import org.gradle.api.Project
import java.io.Serializable
import java.net.URL

interface AemInstance : Serializable {

    companion object {

        val FILTER_ANY = PropertyParser.FILTER_DEFAULT

        val FILTER_LOCAL = "local-*"

        val FILTER_AUTHOR = "*-author"

        val ENVIRONMENT_CMD = "cmd"

        val URL_AUTHOR_DEFAULT = "http://localhost:4502"

        val URL_PUBLISH_DEFAULT = "http://localhost:4503"

        val USER_DEFAULT = "admin"

        val PASSWORD_DEFAULT = "admin"

        fun parse(str: String): List<AemInstance> {
            return str.split(";").map { line ->
                val parts = line.split(",")

                when (parts.size) {
                    4 -> {
                        val (url, type, user, password) = parts
                        AemRemoteInstance(url, user, password, ENVIRONMENT_CMD, type)
                    }
                    3 -> {
                        val (url, user, password) = parts
                        AemRemoteInstance(url, user, password, ENVIRONMENT_CMD, AemInstanceType.byUrl(url).name)
                    }
                    else -> {
                        throw AemException("Cannot parse instance string: '$str'")
                    }
                }
            }
        }

        fun defaults(): List<AemInstance> {
            return listOf(
                    AemLocalInstance(URL_AUTHOR_DEFAULT),
                    AemLocalInstance(URL_PUBLISH_DEFAULT)
            )
        }

        fun filter(project: Project, instanceFilter: String = FILTER_LOCAL): List<AemInstance> {
            val config = AemConfig.of(project)
            val instanceValues = project.properties["aem.deploy.instance.list"] as String?
            if (!instanceValues.isNullOrBlank()) {
                return parse(instanceValues!!)
            }

            val instances = if (config.instances.isEmpty()) {
                return defaults()
            } else {
                config.instances
            }

            return instances.filter { instance ->
                PropertyParser(project).filter(instance.name, "aem.deploy.instance.name", instanceFilter)
            }
        }

        fun portOfUrl(url: String): Int {
            return URL(url).port
        }

    }

    val httpUrl: String

    val httpPort: Int
        get() = portOfUrl(httpUrl)

    val user: String

    val password: String

    val hiddenPassword: String
        get() = "*".repeat(password.length)

    val environment: String

    val typeName: String

    val type: AemInstanceType
        get() = AemInstanceType.byName(typeName)

    val credentials: String
        get() = "$user:$password"

    val name: String
        get() = "$environment-$typeName"

    fun validate() {
        if (!Formats.URL_VALIDATOR.isValid(httpUrl)) {
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

        if (typeName.isBlank()) {
            throw AemException("Type cannot be blank in $this")
        }
    }

}