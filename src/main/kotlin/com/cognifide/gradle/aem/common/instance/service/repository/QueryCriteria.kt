package com.cognifide.gradle.aem.common.instance.service.repository

@Suppress("TooManyFunctions")
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

    fun damAsset() = type("dam:Asset")

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

    fun propertyWhere(name: String, value: String? = null, operation: String? = null) = propertyIndex { p ->
        params["${p}_property"] = name
        if (value != null) {
            params["${p}_property.value"] = value
        }
        if (operation != null) {
            params["${p}_property.operation"] = operation
        }
    }

    fun property(name: String, value: String) = propertyWhere(name, value)

    fun propertyLike(name: String, value: String) = propertyWhere(name, value, "like")

    fun propertyExists(name: String) = propertyWhere(name, null, "exists")

    fun propertyNotExists(name: String) = propertyWhere(name, null, "not")

    fun propertyEquals(name: String, value: String, ignoreCase: Boolean = false) = when (ignoreCase) {
        true -> propertyWhere(name, value, "equalsIgnoreCase")
        false -> propertyWhere(name, value, "equals")
    }

    fun propertyUnequals(name: String, value: String, ignoreCase: Boolean = false) = when (ignoreCase) {
        true -> propertyWhere(name, value, "unequalsIgnoreCase")
        false -> propertyWhere(name, value, "unequals")
    }

    fun propertyIndex(definition: (Int) -> Unit) {
        definition(propertyIndex)
    }

    fun propertyContains(name: String, values: Iterable<String>, all: Boolean = true) = propertyIndex { pi ->
        params["${pi}_property"] = name
        if (all) {
            params["${pi}_property.and"] = "true"
        }
        values.forEachIndexed { vi, value ->
            params["${pi}_property.${vi + 1}_value"] = value
        }
    }

    fun propertyAll(name: String, values: Iterable<String>) = propertyContains(name, values, true)

    fun propertyAny(name: String, values: Iterable<String>) = propertyContains(name, values, false)

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

    // Rest

    val queryString get() = (params + forcedParams).entries
            .joinToString("&") { (k, v) -> "$k=$v" }

    fun copy() = QueryCriteria().apply { params.putAll(this@QueryCriteria.params) }

    fun forMore() = copy().apply {
        offset(offset + limit)
    }

    override fun toString(): String = "QueryCriteria($queryString)"
}
