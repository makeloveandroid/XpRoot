package task

import java.io.File

class DeleteMetaInfoTask(val unZipDir: File) : Task<File, File>() {
    override fun execute(): File {
        val metaInfoFilePath = "META-INF"
        val metaInfoFileRoot = File(unZipDir, metaInfoFilePath)
        if (!metaInfoFileRoot.exists()) {
            return unZipDir
        }
        val childFileList: Array<File> = metaInfoFileRoot.listFiles()
        if (childFileList.isEmpty()) {
            return unZipDir
        }
        for (file in childFileList) {
            val fileName = file.name.toUpperCase()
            if (fileName.endsWith(".MF") || fileName.endsWith(".RAS") || fileName.endsWith(".SF")) {
                file.delete()
            }
        }
        return unZipDir
    }

    override fun complete(result: File) {
    }
}
