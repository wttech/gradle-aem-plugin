package com.cognifide.gradle.aem.test.integration

import com.cognifide.gradle.aem.common.AemException
import com.cognifide.gradle.aem.common.file.FileException
import com.cognifide.gradle.aem.common.file.transfer.FileTransfer
import org.apache.commons.io.FileUtils
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File
import java.util.*

abstract class FileTransferTest {

    abstract fun transfer(): FileTransfer
    abstract fun invalidTransfer(): FileTransfer

    private fun fileIo(resource: String) = File(this::class.java.classLoader.getResource(resource).file)
    private fun text(file: File): String = FileUtils.readFileToString(file, "UTF8")
    private fun tmpFile() = File("${System.getProperty("java.io.tmpdir")}/${UUID.randomUUID()}")

    @AfterEach
    fun truncate() = transfer().truncate()

    @Test
    fun shouldUploadFile() {
        //given
        val transfer: FileTransfer = transfer()
        val source = fileIo("com/cognifide/gradle/aem/test/upload/source.txt")

        //when
        transfer.upload(source)

        //then
        val downloaded = tmpFile()
        transfer.download("source.txt", downloaded)

        Assertions.assertTrue(downloaded.exists())
        Assertions.assertTrue(text(downloaded).contains("Some text"))
    }

    @Test
    fun shouldDeleteFile() {
        //given
        val transfer: FileTransfer = transfer()
        val source = fileIo("com/cognifide/gradle/aem/test/upload/source.txt")

        //when
        transfer.upload(source)
        transfer.delete("source.txt")

        //then
        val downloaded = tmpFile()
        try {
            transfer.download("source.txt", downloaded)
            Assertions.fail<FileTransferSftpTest>()
        } catch (e: FileException) {
            //pass
        }
    }

    @Test
    fun shouldListAllAvailableBackups() {
        //given
        val transfer: FileTransfer = transfer()
        val source = fileIo("com/cognifide/gradle/aem/test/upload/source.txt")
        val source2 = fileIo("com/cognifide/gradle/aem/test/upload/source2.txt")
        transfer.upload(source)
        transfer.upload(source2)

        //when
        val backups = transfer.list()

        //then
        Assertions.assertTrue(backups.size == 2)
        Assertions.assertTrue(backups.containsAll(listOf("source.txt", "source2.txt")))
    }

    @Test
    fun shouldReturnEmptyListOfBackups() {
        //given
        val transfer: FileTransfer = transfer()

        //when
        val backups = transfer.list()

        //then
        Assertions.assertTrue(backups.isEmpty())
    }

    @Test
    fun shouldHandleWhenUploadUrlIsNotAnDirectory() {
        //given

        //when
        try {
            invalidTransfer()

            //then
            Assertions.fail<FileTransferTest>()
        } catch (e: AemException) {
            //pass
        }
    }

}