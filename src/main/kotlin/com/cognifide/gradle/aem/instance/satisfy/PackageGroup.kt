package com.cognifide.gradle.aem.instance.satisfy

import com.cognifide.gradle.aem.internal.file.resolver.FileGroup
import com.cognifide.gradle.aem.internal.file.resolver.FileResolver

class PackageGroup(resolver: FileResolver, name: String) : FileGroup(resolver, name) {

    var awaitAfter = true

    var restartAfter = false

}
