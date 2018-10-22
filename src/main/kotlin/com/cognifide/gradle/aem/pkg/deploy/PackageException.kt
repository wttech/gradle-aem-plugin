package com.cognifide.gradle.aem.pkg.deploy

import com.cognifide.gradle.aem.api.AemException

class PackageException(message: String) : AemException(message) {

    companion object {
        fun of(errors: Set<PackageError>):PackageException{
            //TODO create message
            return PackageException(errors.toString())
        }
    }
}