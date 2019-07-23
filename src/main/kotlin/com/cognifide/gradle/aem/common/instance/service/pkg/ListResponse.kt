package com.cognifide.gradle.aem.common.instance.service.pkg

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class ListResponse private constructor() {

    lateinit var results: List<Package>

    fun resolvePackage(expected: Package): Package? {
        return PackageResolver.values()
                .asSequence()
                .map { it.resolve(this, expected) }
                .firstOrNull { it != null }
    }
}
