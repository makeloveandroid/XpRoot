package task

import util.FileUtils
import java.io.File

/**
 * copy 模块在model中
 */
class CopyXpModelTask(val unZipDir: File, val models: List<String>) : Task<File, File>() {

    companion object {
        // 和 XpRoot 中的 XPOSED_MODULE_FILE_NAME_PREFIX 一样哦
        private const val XPOSED_MODULE_FILE_NAME_PREFIX = "libxproot_xp_module_"
    }

    override fun execute(): File {
        /**
         * so目录
         */
        val libFile = File(unZipDir, "lib")
        val libFiles = libFile.listFiles()
        for (file in libFiles) {
            models.forEachIndexed { index, modelFile ->
                val destFile = File(file, "$XPOSED_MODULE_FILE_NAME_PREFIX${index}.so")
                FileUtils.copy(File(modelFile), destFile)
            }
        }
        return unZipDir
    }

    override fun complete(result: File) {
    }
}