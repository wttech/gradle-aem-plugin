package com.cognifide.gradle.aem.environment

import com.cognifide.gradle.aem.environment.checks.HealthChecks
import com.cognifide.gradle.aem.environment.docker.DockerOptions
import com.cognifide.gradle.aem.environment.hosts.HostsOptions
import java.net.HttpURLConnection.HTTP_OK

class EnvironmentOptions {
    var healthChecks = HealthChecks()
    val hosts = HostsOptions()
    val docker = DockerOptions()

    fun healthChecks(configurer: HealthChecks.() -> Unit) {
        healthChecks = HealthChecks().apply(configurer)
    }

    fun hosts(config: Map<String, String>) {
        hosts.configure(config)
    }

    fun docker(configurer: DockerOptions.() -> Unit) {
        docker.apply(configurer)
    }

    init {
        healthChecks {
            "http://example.com/en-us.html" respondsWith {
                status = HTTP_OK
                text = "English"
            }
            "http://demo.example.com/en-us.html" respondsWith {
                status = HTTP_OK
                text = "English"
            }
            "http://author.example.com/libs/granite/core/content/login.html" +
                    "?resource=%2F&\$\$login\$\$=%24%24login%24%24&j_reason=unknown&j_reason_code=unknown" respondsWith {
                status = HTTP_OK
                text = "AEM Sign In"
            }
        }
    }
}

