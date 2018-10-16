package com.cognifide.gradle.aem.pkg.deploy

import com.cognifide.gradle.aem.api.AemException

class DeployException : AemException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)

    constructor(message: String, errors: List<String>) : super(message){
        this.errors = errors
    }

    lateinit var errors:List<String>
}
