package com.cognifide.gradle.aem.common.instance.service.repository

class Query {

    val params = mutableMapOf<String, String>()

    fun path(value: String) {
        params["path"] = value
    }

    fun orderBy(value: String) {
        params["orderby"] = value
    }

    fun page(value: Int) {
        params["page"] = value.toString() // TODO to be checked 'name'
    }

    val page: Int get() = params["page"]?.toInt() ?: 1

    fun nextPage() = page(page + 1)

    fun previousPage() = page(page - 1)

    fun property(name: String, value: String, operation: String = "like") {
        propertyIndex.let { i ->
            params.putAll(mapOf(
                    "${i}_property" to name,
                    "${i}_property.value" to value,
                    "${i}_property.operation" to operation
            ))
        }
    }

    val propertyIndex get() = params.keys
            .filter { it.endsWith("_property") }
            .map { it.split("_")[0].toInt() }
            .max()

    val queryString get() = params.entries.joinToString("&") { (k, v) -> "$k=$v" }
}