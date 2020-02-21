package com.cognifide.gradle.aem.pkg.tasks.compose

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

interface BundleInstalled : RepositoryArchive {

    @get:Input
    @get:Optional
    val runMode: Property<String>
}
