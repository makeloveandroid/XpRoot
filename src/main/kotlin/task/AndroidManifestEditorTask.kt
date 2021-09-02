package task

import com.wind.meditor.core.FileProcesser
import com.wind.meditor.property.AttributeItem
import com.wind.meditor.property.ModificationProperty
import com.wind.meditor.utils.NodeValue
import task.GetApplicationTask.Companion.ANDROID_MANIFEST
import util.FileUtils
import util.Log
import java.io.File

class AndroidManifestEditorTask(private val isDebug: Boolean, private val newApplication: String, val unZipFile: File) : Task<File, File>() {
    companion object {
        const val PROXY_APPLICATION_NAME = "com.ks.XpApplication"
        private const val ANDROID_MANIFEST_TMP = "AndroidManifest_tmp.xml"
    }

    override fun execute(): File {
        val property = ModificationProperty()
        if (isDebug) {
            // 增加debug属性
            property.addApplicationAttribute(AttributeItem(NodeValue.Application.DEBUGGABLE, true))
        }

        if (newApplication.isNotEmpty()) {
            // 增加Application
            property.addApplicationAttribute(
                    AttributeItem(
                            NodeValue.Application.NAME,
                            PROXY_APPLICATION_NAME
                    )
            )
        }

        val newManifest = File(unZipFile.parent, ANDROID_MANIFEST_TMP)
        FileProcesser.processManifestFile(
                File(unZipFile, ANDROID_MANIFEST).absolutePath,
                newManifest.absolutePath,
                property
        )
        return newManifest
    }

    override fun complete(result: File) {
        Log.d("AndroidManifestEditorTask", "得到新的AndroidManifest  ${result.absolutePath}")
        // 删除原来的替换新
        val oldManifest = File(unZipFile, ANDROID_MANIFEST)
        if (oldManifest.exists()) {
            oldManifest.delete()
        }
        oldManifest.writeBytes(result.readBytes())
    }
}