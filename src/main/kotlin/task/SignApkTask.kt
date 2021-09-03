package task

import com.android.apksigner.ApkSignerTool
import com.wind.meditor.utils.ShellCmdUtil
import util.Log
import java.io.File
import java.util.*

class SignApkTask(val unsignApk: File) : Task<File, File>() {
    override fun execute(): File {
        Log.d("SignApkTask", "开始重新签名App!!!")
        // copy 签名
        val singFile = File(unsignApk.parent, "keystore").apply {
            writeBytes(Thread.currentThread().contextClassLoader.getResourceAsStream("keystore").readBytes())
        }

        val signApk = File(unsignApk.parent, "app-signed.apk")
        signApk(
            unsignApk.absolutePath,
            singFile.absolutePath, signApk.absolutePath
        )
        return signApk
    }

    // 使用Android build-tools里自带的apksigner工具进行签名
    private fun signApkUsingAndroidApksigner(
        apkPath: String,
        keyStorePath: String,
        signedApkPath: String,
        keyStorePassword: String
    ): Boolean {
        val commandList = ArrayList<String>()
        commandList.add("sign")
        commandList.add("--ks")
        commandList.add(keyStorePath)
        commandList.add("--ks-key-alias")
        commandList.add("key0")
        commandList.add("--ks-pass")
        commandList.add("pass:$keyStorePassword")
        commandList.add("--key-pass")
        commandList.add("pass:$keyStorePassword")
        commandList.add("--out")
        commandList.add(signedApkPath)
        commandList.add("--v1-signing-enabled")
        commandList.add("true")
        commandList.add("--v2-signing-enabled") // v2签名不兼容android 6
        commandList.add("false")
        commandList.add("--v3-signing-enabled") // v3签名不兼容android 6
        commandList.add("false")
        commandList.add(apkPath)
        val size = commandList.size
        var commandArray: Array<String?>? = arrayOfNulls(size)
        commandArray = commandList.toArray(commandArray)
        try {
            ApkSignerTool.main(commandArray)
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return true
    }

    private fun signApk(
        apkPath: String,
        keyStorePath: String,
        signedApkPath: String
    ): Boolean {
        if (signApkUsingAndroidApksigner(apkPath, keyStorePath, signedApkPath, "123456")) {
            return true
        }
        return try {
            val time = System.currentTimeMillis()
            val keystoreFile = File(keyStorePath)
            if (keystoreFile.exists()) {
                val signCmd: StringBuilder
                signCmd = StringBuilder("jarsigner ")
                signCmd.append(" -keystore ")
                    .append(keyStorePath)
                    .append(" -storepass ")
                    .append("123456")
                    .append(" -signedjar ")
                    .append(" $signedApkPath ")
                    .append(" $apkPath ")
                    .append(" -digestalg SHA1 -sigalg SHA1withRSA ")
                    .append(" key0 ")
                //                System.out.println("\n" + signCmd + "\n");
                val result: String = ShellCmdUtil.execCmd(signCmd.toString(), null)
                println(
                    """ sign apk time is :${(System.currentTimeMillis() - time) / 1000}s

  result=$result"""
                )
                return true
            }
            println(
                """ keystore not exist :${keystoreFile.absolutePath} please sign the apk by hand. 
"""
            )
            false
        } catch (e: Throwable) {
            println(
                "use default jarsigner to sign apk failed, fail msg is :" +
                        e.toString()
            )
            false
        }
    }

    override fun complete(result: File) {
        Log.d("SignApkTask", "APP 签名完成 ${result.absolutePath}")
    }

}