package task

import com.sun.org.apache.bcel.internal.generic.IFEQ
import java.io.File

/**
 * copy xp的核心文件到 app 中
 */
class CopyXpCoreTask(val unZipDir: File) : Task<File, File>() {
    companion object {
        const val DEX_FILE_SO_NAME = "libxprootcore.so"

        val soFlies = mutableMapOf<String, List<String>>().apply {
            put("arm64-v8a", arrayListOf("libsandhook-arm64-v8a.so", "libsandhook-native-arm64-v8a.so"))
            put("armeabi-v7a", arrayListOf("libsandhook-armeabi-v7a.so", "libsandhook-native-armeabi-v7a.so"))
        }
        val dexFile = "xpcore.dex"
    }

    override fun execute(): File {
        /**
         * so目录
         */
        val libFile = File(unZipDir, "lib")
        if (libFile.exists() && libFile.listFiles().isNotEmpty()) {
            val libFiles = libFile.listFiles()
            for (file in libFiles) {
                soFlies[file.name]?.let {
                    // copy so目录
                    for (soFile in it) {
                        val soFileStream = Thread.currentThread().contextClassLoader.getResourceAsStream(soFile)
                        val soFileName = soFile.replace("-${file.name}", "")
                        // 文件写入
                        File(file, soFileName).writeBytes(soFileStream.readBytes())
                    }
                }

                // copy dex
                val dexFileStream = Thread.currentThread().contextClassLoader.getResourceAsStream(dexFile)
                File(file, DEX_FILE_SO_NAME).writeBytes(dexFileStream.readBytes())
            }
        } else {
            // 如果没有 ArmeabiV7a copy一份
            val armeabiV7aDir = File(libFile, "armeabi-v7a")
            armeabiV7aDir.mkdirs()
            soFlies["armeabi-v7a"]?.let {
                // copy so目录
                for (soFile in it) {
                    val soFileStream = Thread.currentThread().contextClassLoader.getResourceAsStream(soFile)
                    val soFileName = soFile.replace("-${armeabiV7aDir.name}", "")
                    // 文件写入
                    File(armeabiV7aDir, soFileName).writeBytes(soFileStream.readBytes())
                }
            }

            // copy dex
            val dexFileStream = Thread.currentThread().contextClassLoader.getResourceAsStream(dexFile)
            File(armeabiV7aDir, DEX_FILE_SO_NAME).writeBytes(dexFileStream.readBytes())
        }
        return unZipDir
    }

    override fun complete(result: File) {
    }
}