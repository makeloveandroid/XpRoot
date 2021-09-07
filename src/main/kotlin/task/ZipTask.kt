package task

import util.ApkZipUtils
import util.Log
import util.ZipUtils
import util.ZipUtils.zipFiles
import java.io.File
import java.util.zip.CRC32
import java.util.zip.CheckedOutputStream
import java.util.zip.ZipOutputStream

class ZipTask(val unZipDir: File, val uncompressedFilesOrExts: ArrayList<String>) : Task<File, File>() {
    override fun execute(): File {
        val unsignedApp = File(unZipDir.parent, "app-unsigned.apk")
        Log.d("ZipTask","重新压缩${unsignedApp.absolutePath}")

        val cos = CheckedOutputStream(unsignedApp.outputStream(), CRC32())
        val zipOut = ZipOutputStream(cos)
        val listFiles = unZipDir.listFiles()
        ApkZipUtils.zipFiles(listFiles.asList(), unsignedApp, zipOut, null, uncompressedFilesOrExts)
        return unsignedApp
    }

    override fun complete(result: File) {
        Log.d("ZipTask","重新压缩完成${result.absolutePath}")
    }

}