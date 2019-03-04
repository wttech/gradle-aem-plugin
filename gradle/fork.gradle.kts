import com.neva.gradle.fork.ForkExtension

configure<ForkExtension> {
    properties {
        define(mapOf(
                "version" to {
                    label = "Version"
                    description = "Format 'X.Y.Z' (major.minor.patch)"
                    dynamic()
                }

        ))
    }
    inPlaceConfig("version") {
        eachTextFiles("**/*.gradle.kts") {
            amend {
                it.replace(
                        "implementation\\(\"com.cognifide.gradle:aem-plugin:(\\d.\\d.\\d)\")\\)",
                        render("implementation(\"com.cognifide.gradle:aem-plugin:{{version}}\")")
                )
            }
        }
    }
}
