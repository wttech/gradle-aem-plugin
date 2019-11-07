package com.cognifide.gradle.aem.common.file.transfer

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.utils.Patterns

abstract class ProtocolFileTransfer(aem: AemExtension) : AbstractFileTransfer(aem) {

    abstract val protocols: List<String>

    override fun handles(fileUrl: String): Boolean {
        return !fileUrl.isBlank() && Patterns.wildcard(fileUrl, protocols)
    }
}
