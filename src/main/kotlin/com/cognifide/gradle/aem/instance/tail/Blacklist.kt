package com.cognifide.gradle.aem.instance.tail

import com.cognifide.gradle.aem.common.file.FileOperations

class Blacklist(
    private val filters: List<(Log) -> Boolean> = emptyList(),
    private val blacklists: List<String> = emptyList()
) {

    private val parser = Parser()

    private val blacklist = loadBlacklists()

    private fun loadBlacklists(): Map<String, Log> {
        return (loadDefaultBlacklists(TailOptions.BLACKLIST_FILES_DEFAULT) +
                loadConfiguredBlacklists(blacklists))
                .flatten().map { it.messageChecksum to it }.toMap()
    }

    private fun loadConfiguredBlacklists(blacklists: List<String>): List<List<Log>> {
        return blacklists.map { logFile ->
            FileOperations.fromPathOrClasspath(logFile) { reader ->
                parser.parseLogs(reader)
            }
        }
    }

    private fun loadDefaultBlacklists(blacklists: List<String>): List<List<Log>> {
        return blacklists.mapNotNull { logFile ->
            FileOperations.optionalFromPathOrClasspath(logFile) { reader ->
                parser.parseLogs(reader)
            }
        }
    }

    fun isBlacklisted(log: Log) = blacklist.containsKey(log.messageChecksum) || !filters.none { it(log) }
}