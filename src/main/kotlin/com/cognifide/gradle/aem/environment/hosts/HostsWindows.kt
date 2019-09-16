package com.cognifide.gradle.aem.environment.hosts

import com.cognifide.gradle.aem.environment.tasks.EnvironmentHosts

class HostsWindows(list: List<Host>) : Hosts(
        defined = list,
        filePath = HOSTS_FILE,
        permissionDeniedText = PERMISSION_DENIED_SYSTEM_TEXT,
        superUserRequestMessage = SUPER_USER_REQUEST_MESSAGE
) {

    companion object {

        const val HOSTS_FILE = "C:\\Windows\\System32\\drivers\\etc\\hosts"

        const val PERMISSION_DENIED_SYSTEM_TEXT = "Access is denied"

        const val SUPER_USER_REQUEST_MESSAGE = "Editing hosts requires admin privileges," +
                " please run PowerShell 'As Administrator' and exec `.\\gradlew.bat ${EnvironmentHosts.NAME}`"
    }
}
