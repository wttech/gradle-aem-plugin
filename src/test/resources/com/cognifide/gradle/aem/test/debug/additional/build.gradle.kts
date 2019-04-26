plugins {
    id("com.cognifide.aem.bundle")
}

group = "com.company.aem"
description = "Additional"

repositories {
    jcenter()
}

dependencies {
    compile(group = "org.osgi", name = "osgi.cmpn", version = "6.0.0")
}

aem {
    config {
        environment {
            directories(
                    "logs",
                    "cache/content/example/live",
                    "cache/content/example/demo"
            )
            healthChecks {

                "http://example.com/en-us.html" respondsWith {
                    status = 200
                    text = "English"
                }
                "http://demo.example.com/en-us.html" respondsWith {
                    status = 200
                    text = "English"
                }
                "http://author.example.com/libs/granite/core/content/login.html" +
                        "?resource=%2F&\$\$login\$\$=%24%24login%24%24&j_reason=unknown&j_reason_code=unknown" respondsWith {
                    status = 200
                    text = "AEM Sign In"
                }
            }
        }

        // custom env, no ports, by domain name
        remoteInstance("http://author.example.com") {
            environment = "prod"
            typeName = "author"
        }
        remoteInstance("http://example.com") {
            environment = "prod"
            typeName = "publish"
        }

        // custom env, no ports, by IP
        remoteInstance("http://192.168.1.1") {
            typeName = "author"
            environment = "int"
            property("externalUrl", "http://author.aem.local")
        }
        remoteInstance("http://192.168.1.2") {
            typeName = "publish"
            environment = "int"
            property("externalUrl", "http://aem.local")
        }

        // custom env, ports and credentials
        remoteInstance("https://192.168.3.1:8082") {
            typeName = "author"
            environment = "stg"
            user = "user1"
            password = "password1"
        }
        remoteInstance("https://192.168.3.2:8083") {
            typeName = "publish"
            environment = "stg"
            user = "user2"
            password = "password2"
        }

        // custom ports but same url, multiple instances of same type
        remoteInstance("http://192.168.2.1:4502") {
            typeName = "author-1"
            environment = "perf"
        }
        remoteInstance("http://192.168.2.1:5502") {
            typeName = "author-2"
            environment = "perf"
        }
        remoteInstance("http://192.168.2.2:4503") {
            typeName = "publish-1"
            environment = "perf"
        }
        remoteInstance("http://192.168.2.2:5503") {
            typeName = "publish-2"
            environment = "perf"
        }
    }
}