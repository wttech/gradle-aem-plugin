plugins {
    id("com.cognifide.aem.instance.local") version "14.4.1"
}

aem {
    instance {
        provisioner {
            enableCrxDe()
            deployPackage("https://github.com/Adobe-Consulting-Services/acs-aem-commons/releases/download/acs-aem-commons-4.0.0/acs-aem-commons-content-4.0.0-min.zip")
            deployPackage("org.jetbrains.kotlin:kotlin-osgi-bundle:1.4.10")
            deployPackage("https://github.com/icfnext/aem-groovy-console/releases/download/13.0.0/aem-groovy-console-13.0.0.zip")
        }
    }
}