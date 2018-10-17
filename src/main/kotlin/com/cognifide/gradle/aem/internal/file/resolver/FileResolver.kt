package com.cognifide.gradle.aem.internal.file.resolver

import org.gradle.api.Project
import java.io.File

class FileResolver(project: Project, downloadDir: File) : Resolver<FileGroup>(project, downloadDir) {

    override fun createGroup(name: String): FileGroup {
        return FileGroup(this, name)
    }

}