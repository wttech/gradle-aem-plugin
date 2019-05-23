package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.instance.service.pkg.Package
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.gradle.api.Project

@JsonIgnoreProperties(ignoreUnknown = true)
class ListResponse private constructor() {

    lateinit var results: List<Package>

    fun resolvePackage(project: Project, expected: Package): Package? {
        return PackageResolver.values()
                .asSequence()
                .map { it.resolve(project, this, expected) }
                .firstOrNull { it != null }
    }
}
