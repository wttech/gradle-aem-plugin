package com.cognifide.gradle.aem.pkg.deploy

enum class CriticalInstallationError(val className: String) {
    CONSTRAINT_VIOLATION_EXCEPTION("javax.jcr.nodetype.ConstraintViolationException"),
    DEPENDENCY_EXCEPTION("org.apache.jackrabbit.vault.packaging.DependencyException");

    companion object {
        fun findCriticalErrorsIn(errors: List<String>): Set<String> {
            val result = HashSet<String>()
            for (error in errors) {
                for (critical in values()) {
                    if (error.contains(critical.className)) {
                        result.add(critical.className)
                    }
                }
            }
            return result
        }
    }

}