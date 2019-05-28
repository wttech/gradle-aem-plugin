package com.cognifide.gradle.aem.common.instance

import com.cognifide.gradle.aem.AemException

enum class LocalInstanceSource {
    /**
     * Create instances from most recent backup (external or internal)
     * or fallback to creating from the scratch if there is no backup available.
     */
    AUTO,
    /**
     * Force creating instances from the scratch.
     */
    SCRATCH,
    /**
     * Force using backup available at external source (specified in 'localInstance.zipUrl').
     */
    BACKUP_EXTERNAL,
    /**
     * Force using internal backup (created by task 'instanceBackup').
     */
    BACKUP_INTERNAL;

    companion object {
        fun of(name: String): LocalInstanceSource {
            return values().find { it.name.equals(name, true) }
                    ?: throw AemException("Unsupported local instance source: $name")
        }
    }
}