package task

import util.Log
import util.ZipUtils
import java.io.File
import java.util.zip.CRC32
import java.util.zip.CheckedOutputStream
import java.util.zip.ZipOutputStream

class ZipTask(val unZipDir: File) : Task<File, File>() {
    override fun execute(): File {
        val unsignedApp = File(unZipDir.parent, "app-unsigned.apk")
        val cos = CheckedOutputStream(unsignedApp.outputStream(), CRC32())
        val zipOut = ZipOutputStream(cos)
        val listFiles = unZipDir.listFiles()
        ZipUtils.zipFiles(listFiles.asList(), unsignedApp, zipOut, null)
        return unsignedApp
    }

    override fun complete(result: File) {
        Log.d("ZipTask","重新压缩${result.absolutePath}")
    }


}