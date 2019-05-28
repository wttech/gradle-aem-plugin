package com.cognifide.gradle.aem.common.file.resolver

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.utils.formats.JsonPassword
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import java.io.Serializable

// TODO remove it
class ResolverOptions(aem: AemExtension) : Serializable {

    var httpUsername: String? = aem.props.string("resolver.http.username")

    @JsonSerialize(using = JsonPassword::class, `as` = String::class)
    var httpPassword: String? = aem.props.string("resolver.http.password")

    var httpConnectionIgnoreSsl: Boolean? = aem.props.boolean("resolver.http.connectionIgnoreSsl")
}