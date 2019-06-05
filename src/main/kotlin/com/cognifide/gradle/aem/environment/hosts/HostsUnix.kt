package com.cognifide.gradle.aem.environment.hosts

import com.cognifide.gradle.aem.environment.tasks.EnvironmentHosts

class HostsUnix(list: List<Host>) : Hosts(
        defined = list,
        filePath = HOSTS_FILE,
        permissionDeniedText = PERMISSION_DENIED_SYSTEM_TEXT,
        superUserRequestMessage = SUPER_USER_REQUEST_MESSAGE
) {

    companion object {

        const val HOSTS_FILE = "/etc/hosts"

        const val PERMISSION_DENIED_SYSTEM_TEXT = "Permission denied"

        const val SUPER_USER_REQUEST_MESSAGE = "Editing hosts requires super user privileges: `sudo ./gradlew ${EnvironmentHosts.NAME}`"
    }
}