package task

import util.Log
import java.io.File

/**
 * 增加调用入口的dex方法
 */
class CopyAppendDexTask(val unZipDir: File) : Task<File, File>() {
    companion object {
        private const  val DEX_FILE = "xp_call_core.dex"
    }

    override fun execute(): File {
        val dexSize = unZipDir.listFiles().filter { it.name.endsWith(".dex") }.size
        Log.d("CopyAppendDexTask", "当前dex的个数为 $dexSize")
        val dexFileStream = Thread.currentThread().contextClassLoader.getResourceAsStream(DEX_FILE)
        // 文件写入
        val appendDexFile = File(unZipDir, "classes${dexSize + 1}.dex").apply {
            writeBytes(dexFileStream.readBytes())
        }
        return appendDexFile
    }

    override fun complete(result: File) {
    }
}