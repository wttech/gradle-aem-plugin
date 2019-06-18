import com.neva.gradle.fork.ForkExtension

configure<ForkExtension> {
    properties {
        define(mapOf(
                "version" to {
                    label = "Version"
                    description = "Format 'X.Y.Z' (major.minor.patch)"
                    dynamic()
                    defaultValue = project.version.toString()
                }

        ))
    }
    inPlaceConfig("version") {
        eachTextFiles("gradle.properties") {
            amend {
                it.replace("version=${project.version}", render("version={{version}}"))
            }
        }
        eachTextFiles(listOf("README.md", "**/*.gradle.kts")) {
            amend {
                it.replace(
                    "implementation(\"com.cognifide.gradle:aem-plugin:${project.version}\")",
                    render("implementation(\"com.cognifide.gradle:aem-plugin:{{version}}\")")
                )
            }
        }
    }
}
