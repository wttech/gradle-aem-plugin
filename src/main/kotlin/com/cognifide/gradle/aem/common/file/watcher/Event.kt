package com.cognifide.gradle.aem.common.file.watcher

import java.io.File

class Event(val file: File, val type: EventType) {

    override fun toString(): String {
        return "$file [${type.name.toLowerCase().replace("_", " ")}]"
    }
}
