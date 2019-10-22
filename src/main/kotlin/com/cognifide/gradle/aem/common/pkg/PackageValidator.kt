package com.cognifide.gradle.aem.common.pkg

import com.cognifide.gradle.aem.AemExtension
import com.fasterxml.jackson.annotation.JsonIgnore
import net.adamcin.granite.client.packman.ACHandling
import net.adamcin.granite.client.packman.validation.DefaultValidationOptions
import net.adamcin.granite.client.packman.validation.ValidationResult
import net.adamcin.granite.client.packman.validation.ValidationResult.Reason
import java.io.File
import net.adamcin.granite.client.packman.validation.PackageValidator as Base

/**
 * CRX package validator.
 *
 * @see <https://github.com/adamcin/granite-client-packman/tree/master/src/main/java/net/adamcin/granite/client/packman/validation>
 */
class PackageValidator(aem: AemExtension) {

    private val logger = aem.logger

    var enabled = aem.props.boolean("aem.package.validation.enabled") ?: true

    var verbose = aem.props.boolean("aem.package.validation.verbose") ?: true

    @JsonIgnore
    var options: DefaultValidationOptions.() -> Unit = {
        aem.props.boolean("aem.package.validation.allowNonCoveredRoots")?.let { isAllowNonCoveredRoots = it }
        aem.props.list("aem.package.validation.forbiddenExtensions")?.let { forbiddenExtensions = it }
        aem.props.list("aem.package.validation.forbiddenFilterRootPrefixes")?.let { forbiddenFilterRootPrefixes = it }
        aem.props.list("aem.package.validation.pathsDeniedForInclusion")?.let { pathsDeniedForInclusion = it }
        aem.props.list("aem.package.validation.forbiddenACHandling")?.let { forbiddenACHandlingModes = acHandlings(it) }
    }

    @Suppress("TooGenericExceptionCaught")
    fun run(file: File): ValidationResult = try {
        Base.validate(file, DefaultValidationOptions().apply(options))
    } catch (e: Exception) {
        throw PackageException("CRX package validator error! Cause: ${e.message}", e)
    }

    fun validate(file: File) {
        if (!enabled) {
            logger.debug("CRX package validation is disabled.")
            return
        }

        val result = run(file)

        if (result.reason == Reason.SUCCESS) {
            logger.info("CRX package '$file' successfully passed validation.")
        } else {
            val message = "CRX package '$file' does not pass validation!\n${Error(result)}"
            if (verbose) {
                throw PackageException(message)
            } else {
                logger.error(message)
            }
        }
    }

    private fun acHandlings(names: List<String>) = names.map { name ->
        ACHandling.values().firstOrNull { it.name.equals(name, true) }
                ?: throw PackageException("Unsupported CRX package AC handling specified: '$name'!")
    }

    class Error(base: ValidationResult) {

        val reason = base.reason.name

        val invalidRoot = base.invalidRoot.toString()

        val coveringRoot = base.coveringRoot.toString()

        val forbiddenEntry = base.forbiddenEntry

        val forbiddenACHandlingMode = base.forbiddenACHandlingMode.name

        val cause = base.cause.message

        override fun toString(): String {
            return "Error(reason='$reason', invalidRoot='$invalidRoot', coveringRoot='$coveringRoot'," +
                    " forbiddenEntry='$forbiddenEntry', forbiddenACHandlingMode='$forbiddenACHandlingMode', cause=$cause)"
        }
    }
}
