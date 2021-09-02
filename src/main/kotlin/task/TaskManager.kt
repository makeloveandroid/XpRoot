package task

import org.apache.commons.cli.CommandLine
import java.io.File

object TaskManager {
    fun call(commandLine: CommandLine) {
        val host = commandLine.getOptionValue("host")
        val virus = commandLine.getOptionValue("virus")
        val debug = commandLine.getOptionValue("debug")
        val dex = commandLine.getOptionValue("dex")
        // 第一步 Copy 1份 Apk
        val unApkFile = UnZipApkTask(host).call()

        // 保存原来的签名文件
        SaveOriginSignTask(File(host), unApkFile).call()

        // 删除以前的签名信息
        DeleteMetaInfoTask(unApkFile).call()

        // 追加dex
        val appendDexFile = CopyAppendDexTask(unApkFile).call()

        // 第二部获取 application
        val applicationName = GetApplicationTask(unApkFile).call()

        // 判断是否要替换或增加 application
        var newApplicationName = ""
        if (dex != "1" || applicationName.isEmpty()) {
            newApplicationName = AndroidManifestEditorTask.PROXY_APPLICATION_NAME
        }

        if (newApplicationName.isNotEmpty() || debug == "1") {
            // 修改 Manifest debug 参数 或者 application 参数
            AndroidManifestEditorTask(debug == "1", newApplicationName, unApkFile).call()
        }

        // 注入hook start方法
        if (applicationName.isNotEmpty()) {
            if (dex == "1") {
                // dex 1 的模式通过修改原有的 application 注入 (可能存在dex方法数超655535)
                ModifyApplicationDexTask(unApkFile, applicationName).call()
            } else {
                // 默认模式通过 替换 继承 Application 方式 (规避掉方法数超 655535) appendDexFile 就是替换的application地址
                ModifyNewApplicationDexTask(unApkFile, appendDexFile, applicationName).call()
            }
        }
        // copy 文件
        CopyXpCoreTask(unApkFile).call()
        // copy xposed 模块
        CopyXpModelTask(unApkFile, mutableListOf(virus)).call()

        // 重新压缩App
        val unsignedApp = ZipTask(unApkFile).call()
        // 重新签名
        SignApkTask(unsignedApp).call()
    }
}