package task

import util.*
import java.io.File
import java.lang.RuntimeException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class UnZipApkData(val file: File, val uncompressedFilesOrExts: ArrayList<String>)

class UnZipApkTask(private val apkPath: String) : Task<String, UnZipApkData>() {
    companion object{
        private const val DATA_PATTERN = "yyyy-MM-dd HH:mm:ss";
        private val sdf = SimpleDateFormat(DATA_PATTERN);
    }

    override fun execute(): UnZipApkData {
        val apk = File(apkPath)
        if (!apk.exists()) {
            throw RuntimeException("宿主APP不存在")
        }
        val dirName = if (IS_DEBUG) {
            "debug"
        } else {
            sdf.format(Date())
        }
        val parent = File(apk.parent, "${File(apkPath).name.getBaseName()}-${dirName}")
        if (parent.exists()) {
            FileUtils.delete(parent)
        }
        val dir = File(parent, "app")
        dir.mkdirs()
        val data = UnZipApkData(dir, ArrayList())
        ApkZipUtils.unzipFile(apk, dir, data.uncompressedFilesOrExts, null)
        return data
    }


    override fun complete(result: UnZipApkData) {
        Log.d("wyz", "解压APP完成:${result.file.absoluteFile}")
    }
}