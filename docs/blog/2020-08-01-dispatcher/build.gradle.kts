plugins {
    id("com.cognifide.aem.instance.local") version "14.2.9"
    id("com.cognifide.environment") version "1.0.2"
}

aem {
    instance {
        provisioner {
            enableCrxDe()
            deployPackage("com.adobe.cq:core.wcm.components.all:2.8.0@zip")
            deployPackage("com.neva.felix:search-webconsole-plugin:1.3.0")
        }
    }
}

environment { // https://github.com/Cognifide/gradle-environment-plugin
    docker {
        containers {
            "dispatcher" {
                resolve {
                    resolveFiles {
                        download("http://download.macromedia.com/dispatcher/download/dispatcher-apache2.4-linux-x86_64-4.3.3.tar.gz").use {
                            copyArchiveFile(it, "**/dispatcher-apache*.so", file("modules/mod_dispatcher.so"))
                        }
                    }
                    ensureDir("htdocs", "cache", "logs")
                }
                up {
                    ensureDir("/usr/local/apache2/logs", "/var/www/localhost/htdocs", "/var/www/localhost/cache")
                    execShell("Starting HTTPD server", "/usr/sbin/httpd -k start")
                }
            }
        }
    }
    hosts {
        "http://example.com" { tag("live") }
    }
}