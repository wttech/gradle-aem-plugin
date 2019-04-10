package com.cognifide.gradle.aem.environment.hosts

import com.cognifide.gradle.aem.environment.tasks.EnvHosts

class HostsUnix(list: List<Host>) : Hosts(
        list = list,
        filePath = HOSTS_FILE,
        permissionDeniedText = PERMISSION_DENIED_SYSTEM_TEXT,
        superUserRequestMessage = SUPER_USER_REQUEST_MESSAGE
) {

    companion object {
        const val HOSTS_FILE = "/etc/hosts"
        const val PERMISSION_DENIED_SYSTEM_TEXT = "Permission denied"
        const val SUPER_USER_REQUEST_MESSAGE = "This very task requires super user privileges: `sudo ./gradlew ${EnvHosts.NAME}`"
    }
}