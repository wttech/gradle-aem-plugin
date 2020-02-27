package com.cognifide.gradle.aem.common.instance.service.repository

@Suppress("TooManyFunctions")
class QueryCriteria {

    // All params

    val params = mutableMapOf<String, String>()

    // Custom params

    fun property(definition: (Int) -> Unit) {
        definition(propertyIndex)
    }

    private val propertyIndex: Int get() = (params.keys
            .filter { it.endsWith("_property") }
            .map { it.split("_")[0].toInt() }
            .max() ?: 0) + 1

    fun group(name: String, values: Iterable<String>, or: Boolean = true) {
        params["group.p.or"] = or.toString()
        values.forEachIndexed { vi, value ->
            params["group.${vi + 1}_$name"] = value
        }
    }

    // Node type filtering params

    fun type(value: String) {
        params["type"] = value
    }

    fun types(vararg values: String) = types(values.asIterable())

    fun types(values: Iterable<String>) {
        values.toList().apply {
            when (size) {
                1 -> type(first())
                else -> group("type", values)
            }
        }
    }

    fun file() = type("nt:file")

    fun page() = type("cq:Page")

    fun pageContent() = type("cq:PageContent")

    fun damAsset() = type("dam:Asset")

    // Specialized filtering params

    fun name(wildcardPattern: String) {
        params["nodename"] = wildcardPattern
    }

    fun path(value: String) {
        params["path"] = value
    }

    fun paths(vararg values: String) = paths(values.asIterable())

    fun paths(values: Iterable<String>) {
        values.toList().apply {
            when (size) {
                1 -> path(first())
                else -> group("path", values)
            }
        }
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

    // Custom property filtering params

    fun propertyWhere(name: String, value: String? = null, operation: String? = null) = property { p ->
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

    fun propertyExists(name: String) = propertyWhere(name, true.toString(), "exists")

    fun propertyNotExists(name: String) = propertyWhere(name, false.toString(), "exists")

    fun propertyNot(name: String) = propertyWhere(name, null, "not")

    fun propertyEquals(name: String, value: String, ignoreCase: Boolean = false) = when (ignoreCase) {
        true -> propertyWhere(name, value, "equalsIgnoreCase")
        false -> propertyWhere(name, value, "equals")
    }

    fun propertyNotEquals(name: String, value: String, ignoreCase: Boolean = false) = when (ignoreCase) {
        true -> propertyWhere(name, value, "unequalsIgnoreCase")
        false -> propertyWhere(name, value, "unequals")
    }

    fun propertyContains(name: String, values: Iterable<String>, all: Boolean = true) = property { pi ->
        params["${pi}_property"] = name
        if (all) {
            params["${pi}_property.and"] = "true"
        }
        values.forEachIndexed { vi, value ->
            params["${pi}_property.${vi + 1}_value"] = value
        }
    }

    fun propertyContains(name: String, vararg values: String, all: Boolean = true) = propertyContains(name, values.asIterable(), all)

    fun propertyContainsAny(name: String, values: Iterable<String>) = propertyContains(name, values, false)

    fun propertyContainsAny(name: String, vararg values: String) = propertyContains(name, values.asIterable(), false)

    // Ordering params

    fun orderBy(value: String, desc: Boolean = false) {
        params["orderby"] = value
        if (desc) {
            params["orderby.sort"] = "desc"
        }
    }

    fun orderByPath(desc: Boolean = false) = orderBy("path", desc)

    fun orderByName(desc: Boolean = false) = orderBy("nodename", desc)

    fun orderByProperty(name: String, desc: Boolean = false) = orderBy("@$name", desc)

    fun orderByScore(desc: Boolean = true) = orderByProperty("jcr:score", desc)

    fun orderByLastModified(desc: Boolean = true) = orderByProperty("cq:lastModified", desc)

    fun orderByContentLastModified(desc: Boolean = true) = orderByProperty("jcr:content/cq:lastModified", desc)

    // Paginating params

    fun offset(value: Int) {
        params["p.offset"] = value.toString()
    }

    val offset: Int get() = params["p.offset"]?.toInt() ?: 0

    fun limit(value: Int) {
        params["p.limit"] = value.toString()
    }

    val limit: Int get() = params["p.limit"]?.toInt() ?: 10

    // Rest

    val queryString get() = (params + FORCED_PARAMS).entries
            .joinToString("&") { (k, v) -> "$k=$v" }

    fun copy() = QueryCriteria().apply { params.putAll(this@QueryCriteria.params) }

    fun forMore() = copy().apply {
        offset(offset + limit)
    }

    override fun toString(): String = "QueryCriteria($queryString)"

    init {
        limit(100) // performance improvement (default is 10)
    }

    companion object {
        private val FORCED_PARAMS = mapOf(
                "p.guessTotal" to "true",
                "p.hits" to "full"
        )
    }
}
