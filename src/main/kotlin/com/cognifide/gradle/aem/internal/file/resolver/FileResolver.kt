package com.cognifide.gradle.aem.internal.file.resolver

import java.io.File
import org.gradle.api.Project

class FileResolver(project: Project, downloadDir: File) : Resolver<FileGroup>(project, downloadDir) {

    override fun createGroup(name: String): FileGroup {
        return FileGroup(downloadDir, name)
    }
}