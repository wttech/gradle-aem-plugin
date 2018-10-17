plugins {
    id("com.cognifide.aem.base")
}

group = "com.company.aem"

aem {
    config {
        remoteInstance("http://author.example.com") {
            environment = "prod"
            typeName = "author"
        }
    }
}