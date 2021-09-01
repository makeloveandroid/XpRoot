package task

import util.Log
import wind.android.content.res.ManifestParser
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class GetApplicationTask(val apkFile: File) : Task<File, String>() {
    companion object {
        const val ANDROID_MANIFEST = "AndroidManifest.xml"
    }

    override fun execute(): String {
        val manifestInput = File(apkFile, ANDROID_MANIFEST).inputStream()
        val value = ManifestParser.parseManifestFile(manifestInput)
        val applicationName = value.applicationName
        val packageName = value.packageName
        return applicationName
    }

    override fun complete(result: String) {
        Log.d("GetApplicationTask", "获取到 application $result")
    }

}