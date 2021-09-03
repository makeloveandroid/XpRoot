package task

import com.sun.org.apache.bcel.internal.generic.RET
import util.ApkSignatureHelper
import util.Log
import java.io.File

class SaveOriginSignTask(val apkFile: File, val unZipFile: File) : Task<File, File>() {
    companion object{
        private const val SIGNATURE_INFO_ASSET_PATH = "assets/xpatch_asset/original_signature_info.ini"
    }
    override fun execute(): File {
        val originalSignature: String = ApkSignatureHelper.getApkSignInfo(apkFile.absolutePath)
        if (originalSignature.isEmpty()) {
            Log.d("SaveOriginSignTask", "获取签名出错!")
            return apkFile
        }
        val signFile = File(unZipFile, SIGNATURE_INFO_ASSET_PATH)
        if (!signFile.parentFile.exists()) {
            signFile.parentFile.mkdirs()
        }


        signFile.writeText(originalSignature)

        return signFile

    }

    override fun complete(result: File) {
        Log.d("SaveOriginSignTask", "写入原始签名  ${result.absolutePath}")
    }


}