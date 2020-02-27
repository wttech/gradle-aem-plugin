package com.cognifide.gradle.aem.common.instance.service.repository

class QueryCriteria {

    val params = mutableMapOf(
            "p.limit" to "100" // performance optimization for batch processing (default is 10)
    )

    private val forcedParams = mapOf(
            "p.guessTotal" to "true",
            "p.hits" to "full"
    )

    // Node type filtering

    fun type(value: String) {
        params["type"] = value
    }

    fun file() = type("nt:file")

    fun page() = type("cq:Page")

    fun pageContent() = type("cq:PageContent")

    // Specialized filtering

    fun name(wildcardPattern: String) {
        params["nodename"] = wildcardPattern
    }

    fun path(value: String) {
        params["path"] = value
    }

    fun depth(value: Int) {
        params["p.nodedepth"] = value.toString()
    }

    fun fullText(value: String, relativePath: String? = null) {
        params["fulltext"] = value
        if (relativePath != null) {
            params["fulltext.relPath"] = relativePath
        }
    }

    fun tag(value: String, property: String? = null) {
        params["tagid"] = value
        if (property != null) {
            params["tagid.property"] = property
        }
    }

    fun contentTag(value: String) = tag(value, "jcr:content/cq:tags")

    // Custom property filtering

    fun property(name: String, values: Iterable<String>, and: Boolean = true) = propertyIndex { pi ->
        params["${pi}_property"] = name
        if (and) {
            params["${pi}_property.and"] = "true"
        }
        values.forEachIndexed { vi, value ->
            params["${pi}_property.${vi + 1}_value"] = value
        }
    }

    fun propertyIndex(definition: (Int) -> Unit) {
        definition(propertyIndex)
    }

    private val propertyIndex: Int get() = (params.keys
            .filter { it.endsWith("_property") }
            .map { it.split("_")[0].toInt() }
            .max() ?: 0) + 1

    // Ordering

    fun orderBy(value: String, sort: String = "asc") {
        params["orderby"] = value
        if (sort != "asc") {
            params["orderby.sort"] = sort
        }
    }

    fun orderByPath() = orderBy("path")

    fun orderByScore() = orderBy("@jcr:score", "desc")

    fun orderByLastModified() = orderBy("@cq:lastModified", "desc")

    fun orderByContentLastModified() = orderBy("@jcr:content/cq:lastModified", "desc")

    fun offset(value: Int) {
        params["p.offset"] = value.toString()
    }

    // Paginating

    val offset: Int get() = params["p.offset"]?.toInt() ?: 0

    fun limit(value: Int) {
        params["p.limit"] = value.toString()
    }

    val limit: Int get() = params["p.limit"]?.toInt() ?: 10

    fun property(name: String, value: String, operation: String? = null) = propertyIndex { p ->
        params["${p}_property"] = name
        params["${p}_property.value"] = value
        if (operation != null) {
            params["${p}_property.operation"] = operation
        }
    }

    val queryString get() = (params + forcedParams).entries
            .joinToString("&") { (k, v) -> "$k=$v" }

    fun copy() = QueryCriteria().apply { params.putAll(this@QueryCriteria.params) }

    fun forMore() = copy().apply {
        offset(offset + limit)
    }

    override fun toString(): String = "QueryCriteria($queryString)"
}
