package com.cognifide.gradle.aem.common.pkg

import net.adamcin.granite.client.packman.validation.ValidationResult

class PackageValidationError(result: ValidationResult) {

    val reason = result.reason.name

    val invalidRoot = result.invalidRoot.toString()

    val coveringRoot = result.coveringRoot.toString()

    val forbiddenEntry = result.forbiddenEntry

    val forbiddenACHandlingMode = result.forbiddenACHandlingMode.name

    val cause = result.cause.message

    override fun toString(): String {
        return "PackageValidationError(reason='$reason', invalidRoot='$invalidRoot', coveringRoot='$coveringRoot'," +
                " forbiddenEntry='$forbiddenEntry', forbiddenACHandlingMode='$forbiddenACHandlingMode', cause=$cause)"
    }
}
