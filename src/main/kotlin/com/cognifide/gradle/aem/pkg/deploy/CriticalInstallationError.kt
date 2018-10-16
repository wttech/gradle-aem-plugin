package com.cognifide.gradle.aem.pkg.deploy

import javax.jcr.nodetype.ConstraintViolationException

enum class CriticalInstallationError(var className: String) {
    CONSTRAINT_VIOLATION_EXCEPTION(ConstraintViolationException().toString()),
    EX("org.apache.jackrabbit.vault.packaging.DependencyException");

    companion object {
        fun isCriticalErrorPresentAt(errors: List<String>): Boolean {
            for (error in errors) {
                for (critical in values()) {
                    if (error.contains(critical.className)) {
                        return true
                    }
                }
            }
            return false
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