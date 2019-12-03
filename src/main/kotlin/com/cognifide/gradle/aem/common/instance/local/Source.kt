package com.cognifide.gradle.aem.common.instance.local

import com.cognifide.gradle.aem.AemException

enum class Source {

    /**
     * Create instances from most recent backup (local or remote) or fallback to creating from the scratch
     * if there is no backup available.
     */
    AUTO,

    /**
     * Force creating instances from the scratch.
     */
    SCRATCH,

    /**
     * Force using backup from any source.
     */
    BACKUP_ANY,

    /**
     * Force using backup available at remote source (available by using 'localInstance.backup.downloadUrl'
     * or 'localInstance.backup.uploadUrl').
     */
    BACKUP_REMOTE,

    /**
     * Force using local backup (created by task 'instanceBackup').
     */
    BACKUP_LOCAL;

    companion object {
        fun of(name: String): Source {
            return values().find { it.name.equals(name, true) }
                    ?: throw AemException("Unsupported local instance source: $name")
        }
    }
}
