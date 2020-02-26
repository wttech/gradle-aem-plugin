package com.cognifide.gradle.aem.common.instance.service.repository

class QueryCriteria {

    private val defaultParams =  mapOf("p.hits" to "full")

    val params = mutableMapOf<String, String>()

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

    fun orderByScore() = orderBy("@jcr:score", "desc")

    fun orderByLastModified() = orderBy("@jcr:content/cq:lastModified", "desc")

    fun offset(value: Int) {
        params["p.offset"] = value.toString()
    }

    val offset: Int get() = params["p.offset"]?.toInt() ?: 0

    fun limit(value: Int) {
        params["p.limit"] = value.toString()
    }

    val limit: Int get() = params["p.limit"]?.toInt() ?: 10

    fun property(name: String, value: String, operation: String? = null) {
        propertyIndex.let { i ->
            params["${i}_property"] = name
            params["${i}_property.value"] = value
            if (operation != null) {
                params["${i}_property.operation"] = operation
            }
        }
    }

    fun property(name: String, values: Iterable<String>, and: Boolean = true) {
        propertyIndex.let { i ->
            params["${i}_property"] = name
            params["${i}_property.and"] = and.toString()
            values.forEachIndexed { v, value ->
                params["${i}_property.${v}_value"] = value
            }
        }
    }

    val propertyIndex get() = params.keys
            .filter { it.endsWith("_property") }
            .map { it.split("_")[0].toInt() }
            .max()

    val queryString get() = (defaultParams + params).entries
            .joinToString("&") { (k, v) -> "$k=$v" }

    fun copy() = QueryCriteria().apply { params.putAll(this@QueryCriteria.params) }

    fun forMore() = copy().apply {
        offset(offset + limit)
    }

    override fun toString(): String = "QueryCriteria($queryString)"
}