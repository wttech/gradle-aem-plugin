package com.cognifide.gradle.aem.common.instance.service.repository

class QueryCriteria {

    val params = mutableMapOf(
            "p.limit" to "100" // performance optimization for batch processing (default is 10)
    )

    private val forcedParams = mapOf(
            "p.guessTotal" to "true",
            "p.hits" to "full"
    )

    fun type(value: String) {
        params["type"] = value
    }

    fun files() = type("nt:file")

    fun pages() = type("cq:Page")

    fun pageContents() = type("cq:PageContent")

    fun fullText(value: String) {
        params["fulltext"] = value
    }

    fun name(pattern: String) {
        params["nodename"] = pattern
    }

    fun path(value: String) {
        params["path"] = value
    }

    fun depth(value: Int) {
        params["p.nodedepth"] = value.toString()
    }

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

    val offset: Int get() = params["p.offset"]?.toInt() ?: 0

    fun limit(value: Int) {
        params["p.limit"] = value.toString()
    }

    val limit: Int get() = params["p.limit"]?.toInt() ?: 10

    fun property(name: String, value: String, operation: String? = null) {
        propertyIndex.let { p ->
            params["${p}_property"] = name
            params["${p}_property.value"] = value
            if (operation != null) {
                params["${p}_property.operation"] = operation
            }
        }
    }

    fun property(name: String, values: Iterable<String>, and: Boolean = true) {
        propertyIndex.let { p ->
            params["${p}_property"] = name
            params["${p}_property.and"] = and.toString()
            values.forEachIndexed { v, value ->
                params["${p}_property.${v + 1}_value"] = value
            }
        }
    }

    val propertyIndex: Int get() = (params.keys
            .filter { it.endsWith("_property") }
            .map { it.split("_")[0].toInt() }
            .max() ?: 0) + 1

    val queryString get() = (params + forcedParams).entries
            .joinToString("&") { (k, v) -> "$k=$v" }

    fun copy() = QueryCriteria().apply { params.putAll(this@QueryCriteria.params) }

    fun forMore() = copy().apply {
        offset(offset + limit)
    }

    override fun toString(): String = "QueryCriteria($queryString)"
}
