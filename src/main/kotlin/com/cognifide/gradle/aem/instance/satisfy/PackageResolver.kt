package com.cognifide.gradle.aem.instance.satisfy

import com.cognifide.gradle.aem.internal.file.resolver.Resolver
import org.gradle.api.Project
import java.io.File

class PackageResolver(project: Project, downloadDir: File) : Resolver<PackageGroup>(project, downloadDir) {

    override fun createGroup(name: String): PackageGroup {
        return PackageGroup(this, name)
    }

}