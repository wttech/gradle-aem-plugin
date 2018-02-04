package com.cognifide.gradle.aem.instance.satisfy

import com.cognifide.gradle.aem.internal.file.resolver.FileGroup
import com.cognifide.gradle.aem.internal.file.resolver.FileResolution
import org.apache.commons.io.FilenameUtils
import java.io.File

class PackageResolution(group: FileGroup, id: String, action: (FileResolution) -> File) : FileResolution(group, id, action) {

    override fun process(file: File): File {
        val origin = super.process(file)

        return when (FilenameUtils.getExtension(file.name)) {
            "jar" -> wrap(origin)
            "zip" -> origin
            else -> throw PackageException("File $origin must have *.jar or *.zip extension")
        }
    }

    fun wrap(jar: File): File {
        val pkg = File(jar.parentFile, "${jar.nameWithoutExtension}.zip")

        // TODO using template and expanding properties wrap jar into CRX package

        return pkg
    }
}