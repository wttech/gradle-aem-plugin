package com.cognifide.gradle.aem.environment

import com.cognifide.gradle.aem.environment.checks.HealthChecks
import com.cognifide.gradle.aem.environment.hosts.HostsOptions
import java.net.HttpURLConnection.HTTP_OK
import org.gradle.api.tasks.Internal

class EnvironmentOptions {
    @Internal
    var healthChecks = HealthChecks()
    @Internal
    val hosts = HostsOptions()

    fun healthChecks(configurer: HealthChecks.() -> Unit) {
        healthChecks = HealthChecks().apply(configurer)
    }

    fun hosts(config: Map<String, String>) {
        hosts.configure(config)
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
