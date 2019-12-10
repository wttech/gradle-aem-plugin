package com.cognifide.gradle.aem.common.file.transfer

interface FileTransferHandler : FileTransfer {

    /**
     * Unique identifier.
     */
    val name: String

    /**
     * When enabled, transfer will be considered when finding transfer handling particular URL.
     */
    val enabled: Boolean

    /**
     * Determines if operations using this transfer could be done in parallel.
     */
    val parallelable: Boolean
}
