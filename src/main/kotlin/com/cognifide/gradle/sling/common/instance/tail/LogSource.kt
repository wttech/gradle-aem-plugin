package com.cognifide.gradle.sling.common.instance.tail

import java.io.BufferedReader

interface LogSource {

    fun <T> readChunk(parser: (BufferedReader) -> List<T>): List<T>
}
