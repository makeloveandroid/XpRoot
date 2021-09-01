package task

import org.apache.commons.cli.CommandLine
import java.io.File

object TaskManager {
    fun call(commandLine: CommandLine) {
        val host = commandLine.getOptionValue("host")
        val virus = commandLine.getOptionValue("virus")
        val debug = commandLine.getOptionValue("debug")
        // 第一步 Copy 1份 Apk
        val unApkFile = UnZipApkTask(host).call()

        // 保存原来的签名文件
        SaveOriginSignTask(File(host), unApkFile).call()

        // 删除以前的签名信息
        DeleteMetaInfoTask(unApkFile).call()

        // 第二部获取 application
        val applicationName = GetApplicationTask(unApkFile).call()
        if (applicationName.isEmpty() || debug == "1") {
            // 修改 Manifest
            AndroidManifestEditorTask(debug == "1", applicationName.isEmpty(), unApkFile).call()
        }
        // 注入hook start方法
        if (applicationName.isNotEmpty()) {
            ModifyApplicationDexTask(unApkFile, applicationName).call()
        }
        // copy 文件
        CopyXpCoreTask(unApkFile).call()
        // copy xposed 模块
        CopyXpModelTask(unApkFile, mutableListOf(virus)).call()
        // 追加dex
        CopyAppendDexTask(unApkFile).call()
        // 重新压缩App
        val unsignedApp = ZipTask(unApkFile).call()
        // 重新签名
        SignApkTask(unsignedApp).call()
    }
}