package com.cognifide.gradle.aem.test.integration

import com.cognifide.gradle.aem.common.AemException
import com.cognifide.gradle.aem.common.file.FileException
import com.cognifide.gradle.aem.common.file.transfer.FileTransfer
import org.apache.commons.io.FileUtils
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File
import java.io.RandomAccessFile
import java.util.*

abstract class FileTransferTest {

    abstract fun transfer(): FileTransfer
    abstract val uploadUrl: String
    abstract val invalidUrl: String

    private fun fileIo(resource: String) = File(this::class.java.classLoader.getResource(resource).file)
    private fun text(file: File): String = FileUtils.readFileToString(file, "UTF8")
    private fun tmpFile() = File("${System.getProperty("java.io.tmpdir")}/${UUID.randomUUID()}")

    @AfterEach
    fun truncate() = transfer().truncate(uploadUrl)

    @Test
    fun shouldUploadFile() {
        //given
        val transfer: FileTransfer = transfer()
        val source = fileIo("com/cognifide/gradle/aem/test/upload/source.txt")

        //when
        transfer.upload(uploadUrl, source)

        //then
        val downloaded = tmpFile()
        transfer.download(uploadUrl, "source.txt", downloaded)

        Assertions.assertTrue(downloaded.exists())
        Assertions.assertTrue(text(downloaded).contains("Some text"))
    }

    @Test
    fun shouldUploadLargeFile() {
        //given
        val hundredMegs = 100L * 1024L * 1024L
        val transfer: FileTransfer = transfer()
        val source = tmpFile()
        val randomFile = RandomAccessFile(source, "rw")
        randomFile.setLength(hundredMegs)

        //when
        transfer.upload(uploadUrl, source)

        //then
        val downloaded = tmpFile()
        transfer.download(uploadUrl, source.name, downloaded)

        Assertions.assertTrue(downloaded.exists())
        Assertions.assertEquals(hundredMegs, downloaded.length())
    }

    @Test
    fun shouldDeleteFile() {
        //given
        val transfer: FileTransfer = transfer()
        val source = fileIo("com/cognifide/gradle/aem/test/upload/source.txt")

        //when
        transfer.upload(uploadUrl, source)
        transfer.delete(uploadUrl, "source.txt")

        //then
        val downloaded = tmpFile()
        try {
            transfer.download(uploadUrl, "source.txt", downloaded)
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
        transfer.upload(uploadUrl, source)
        transfer.upload(uploadUrl, source2)

        //when
        val backups = transfer.list(uploadUrl)

        //then
        Assertions.assertTrue(backups.size == 2)
        Assertions.assertTrue(backups.containsAll(listOf("source.txt", "source2.txt")))
    }

    @Test
    fun shouldReturnEmptyListOfBackups() {
        //given
        val transfer: FileTransfer = transfer()

        //when
        val backups = transfer.list(uploadUrl)

        //then
        Assertions.assertTrue(backups.isEmpty())
    }

    @Test
    fun shouldHandleWhenUploadUrlIsNotAnDirectory() {
        //given

        //when
        try {
            transfer().list(invalidUrl)

            //then
            Assertions.fail<FileTransferTest>()
        } catch (e: AemException) {
            //pass
        }
    }

}