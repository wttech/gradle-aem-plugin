package com.cognifide.gradle.sling.pkg.tasks.compose

import com.cognifide.gradle.sling.common.pkg.vault.FilterType
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import java.io.Serializable

interface RepositoryArchive : Serializable {

    @get:InputFile
    val file: RegularFileProperty

    @get:Input
    val fileName: Property<String>

    @get:Input
    val dirPath: Property<String>

    @get:Input
    val vaultFilter: Property<Boolean>

    @get:Input
    val vaultFilterType: Property<FilterType>

    fun vaultFilterFile() {
        vaultFilterType.set(FilterType.FILE)
    }

    fun vaultFilterDir() {
        vaultFilterType.set(FilterType.DIR)
    }
}
