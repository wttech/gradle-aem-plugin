package com.cognifide.gradle.aem.pkg.deploy

import com.cognifide.gradle.aem.api.AemException

class MalformedPackageException(message: String) : AemException(message) {

    companion object {
        fun of(response: InstallResponse, messagePrefix: String): MalformedPackageException{
            return MalformedPackageException("$messagePrefix ${response.encounteredPackageErrors} \n Errors: ${response.errors}")
        }
    }
}