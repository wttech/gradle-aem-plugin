package com.cognifide.gradle.aem.instance.satisfy

import com.cognifide.gradle.aem.internal.file.resolver.FileGroup
import com.cognifide.gradle.aem.internal.file.resolver.FileResolution
import com.cognifide.gradle.aem.internal.file.resolver.FileResolver
import java.io.File

// TODO add special methods for controlling AEM reloading, awaiting, respect them while satisfying
class PackageGroup(resolver: FileResolver, name: String) : FileGroup(resolver, name) {

    override fun createResolution(id: String, resolver: (FileResolution) -> File): FileResolution {
        return PackageResolution(this, id, resolver)
    }

}
