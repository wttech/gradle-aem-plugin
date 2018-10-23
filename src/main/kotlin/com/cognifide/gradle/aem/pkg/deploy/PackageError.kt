package com.cognifide.gradle.aem.pkg.deploy

enum class PackageError(val className: String) {
    CONSTRAINT_VIOLATION_EXCEPTION("javax.jcr.nodetype.ConstraintViolationException"),
    DEPENDENCY_EXCEPTION("org.apache.jackrabbit.vault.packaging.DependencyException"),
    SAX_EXCEPTION("org.xml.sax.SAXException");

    companion object {
        fun findPackageErrorsIn(errors: List<String>): Set<PackageError> {
            return errors.fold(mutableSetOf()) { results, error ->
                values().forEach { exception ->
                    if (error.contains(exception.className)) results.add(exception)
                }; results
            }
        }

        fun getClassNames(): List<String> {
            return values().fold(mutableListOf()) { results, error ->
                results.add(error.className); results
            }
        }
    }
}