package task

import util.FileUtils
import util.IS_DEBUG
import util.Log
import util.ZipUtils
import java.io.File
import java.lang.RuntimeException
import java.text.SimpleDateFormat
import java.util.*

class UnZipApkTask(private val apkPath: String) : Task<String, File>() {
    companion object{
        private const val DATA_PATTERN = "yyyy-MM-dd HH:mm:ss";
        private val sdf = SimpleDateFormat(DATA_PATTERN);
    }

    override fun execute(): File {
        val apk = File(apkPath)
        if (!apk.exists()) {
            throw RuntimeException("宿主APP不存在")
        }
        val dirName = if (IS_DEBUG) {
            "debug"
        } else {
            sdf.format(Date())
        }
        val parent = File(apk.parent, dirName)
        if (parent.exists()) {
            FileUtils.delete(parent)
        }
        val dir = File(parent, "app")
        dir.mkdirs()
        ZipUtils.unzipFile(apk, dir)
        return dir
    }


    override fun complete(result: File) {
        Log.d("wyz", "解压APP完成:${result.absolutePath}")
    }
}