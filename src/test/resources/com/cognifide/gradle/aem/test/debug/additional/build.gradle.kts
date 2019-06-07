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
    environment {
        hosts(
                "example.com",
                "demo.example.com",
                "author.example.com",
                "invalidation-only"
        )
        directories {
            regular(
                    "httpd/logs"
            )
            cache(
                    "httpd/cache/content/example/live",
                    "httpd/cache/content/example/demo"
            )
        }
        healthChecks {
            url("Live site", "http://example.com/en-us.html", text = "English")
            url("Demo site", "http://demo.example.com/en-us.html", text = "English")
            url("Author login", "http://author.example.com/libs/granite/core/content/login.html" +
                    "?resource=%2F&\$\$login\$\$=%24%24login%24%24&j_reason=unknown&j_reason_code=unknown", text = "AEM Sign In")
        }
    }
    
    instance {
        // custom env, no ports, by domain name
        remote("http://author.example.com") {
            environment = "prod"
            id = "author"
        }
        remote("http://example.com") {
            environment = "prod"
            id = "publish"
        }

        // custom env, no ports, by IP
        remote("http://192.168.1.1") {
            name = "int-author"
            property("externalUrl", "http://author.aem.local")
        }
        remote("http://192.168.1.2") {
            name = "int-publish"
            property("externalUrl", "http://aem.local")
        }

        // custom env, ports and credentials
        remote("https://192.168.3.1:8082") {
            name = "stg-author"
            user = "user1"
            password = "password1"
        }
        remote("https://192.168.3.2:8083") {
            name = "stg-publish"
            user = "user2"
            password = "password2"
        }

        // custom ports but same url, multiple instances of same type
        remote("http://192.168.2.1:4502") {
            name = "perf-author-1"
        }
        remote("http://192.168.2.1:5502") {
            name = "perf-author-2"
            id = "author-2"
        }
        remote("http://192.168.2.2:4503") {
            name = "perf-publish-1"
        }
        remote("http://192.168.2.2:5503") {
            name = "perf-publish-2"
        }
    }
}