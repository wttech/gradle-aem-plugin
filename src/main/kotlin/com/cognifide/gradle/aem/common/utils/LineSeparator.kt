package com.cognifide.gradle.aem.common.utils

/**
 * @see <https://en.wikipedia.org/wiki/Newline#Representations_in_different_character_encoding_specifications>
 */
enum class LineSeparator(val value: String) {

    LF("\n"),
    CRLF("\r\n"),
    CR("\r"),
    LFCR("\n\r"),
    SYSTEM(System.lineSeparator());

    companion object {

        fun string(name: String): String {
            return values().find { it.name.equals(name, true) }?.value
                    ?: throw IllegalArgumentException("Unsupported line separator specified: $name. Valid are: ${values()}")
        }
    }
}
