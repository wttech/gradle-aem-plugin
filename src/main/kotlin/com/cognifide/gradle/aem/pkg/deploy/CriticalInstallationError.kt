package com.cognifide.gradle.aem.pkg.deploy

import javax.jcr.nodetype.ConstraintViolationException

enum class CriticalInstallationError(var className: String) {
    CONSTRAINT_VIOLATION_EXCEPTION(ConstraintViolationException().toString()),
    EX("org.apache.jackrabbit.vault.packaging.DependencyException");

    companion object {
        fun findCriticalErrorsIn(errors: List<String>): List<String> {
            val result = ArrayList<String>()
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


/* TODO !
 - try to refactor Builder class
 -
 pod root
 formaty
 nazwa paczki
 uszkodzic paczke
 ogromny obrazek */