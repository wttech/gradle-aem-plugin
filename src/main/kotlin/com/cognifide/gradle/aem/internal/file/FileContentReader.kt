package com.cognifide.gradle.aem.internal.file

import org.apache.tools.ant.filters.BaseFilterReader
import org.apache.tools.ant.filters.ChainableReader
import org.gradle.api.file.ContentFilterable
import java.io.Reader

class FileContentReader(input: Reader) : BaseFilterReader(input), ChainableReader {

    private var index: Int = 0

    private lateinit var filter: (String) -> String

    private val buffer: CharArray by lazy {
        filter(readFully()).toCharArray()
    }

    override fun read(): Int {
        if (index > -1) {
            if (index < buffer.size) {
                return buffer[index++].toInt()
            }

            index = -1
        }

        return -1
    }

    override fun chain(reader: Reader): Reader {
        val copy = FileContentReader(reader)
        copy.project = this.project
        copy.filter = this.filter

        return copy
    }

    companion object {

        fun props(filter: (String) -> String) = mapOf("filter" to filter)

        fun filter(filterable: ContentFilterable, filter: (String) -> String) {
            filterable.filter(props(filter), FileContentReader::class.java)
        }

    }

}
