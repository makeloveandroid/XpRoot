package task

import ClassDefWrapper
import InjectMethodBuilder.changeInitSuperMethod
import kotlinx.coroutines.*
import org.jf.baksmali.Adaptors.ClassDefinition
import org.jf.baksmali.BaksmaliOptions
import org.jf.baksmali.formatter.BaksmaliWriter
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.DexFileFactory
import org.jf.dexlib2.Opcodes
import org.jf.dexlib2.iface.ClassDef
import org.jf.dexlib2.iface.DexFile
import org.jf.dexlib2.iface.Method
import org.jf.dexlib2.rewriter.DexRewriter
import org.jf.dexlib2.rewriter.Rewriter
import org.jf.dexlib2.rewriter.RewriterModule
import org.jf.dexlib2.rewriter.Rewriters
import org.jf.dexlib2.writer.io.FileDataStore
import org.jf.dexlib2.writer.pool.DexPool
import util.Log
import util.getBaseName
import java.io.File
import java.io.StringWriter

class ModifyNewApplicationDexTask(val unZipFile: File, val appendDexFile: File, val applicationName: String) : Task<File, File>() {
    override fun execute(): File = runBlocking<File> {
        val applicationDexFile = findApplicationDexFile() ?: throw RuntimeException("没有找到 存在 application 的dex")

        // 修改 原有的dex  application 不为final 因为注入的application要继承
        changeApplicationNoFinal(applicationDexFile)

        // 修改我的类父类宿主 Application
        changeMyApplicationSuper(applicationDexFile)

        return@runBlocking appendDexFile
    }

    /**
     * 修改原有的 application 不能为 final
     */
    private fun changeApplicationNoFinal(applicationDexFile: AppDex) {
        val appClassDef = applicationDexFile.appClassDef
        val dexFile = applicationDexFile.dexFile
        val originalDexFile = File(applicationDexFile.path)

        if (appClassDef.accessFlags and AccessFlags.FINAL.value != 0) {
            // 存在 final 标记
            val dexPool = DexPool(dexFile.opcodes)
            // 写入new dex
            for (classDef in dexFile.classes) {
                if (classDef == appClassDef) {
                    val classWrapper = ClassDefWrapper(appClassDef)
                    // 删除掉 final 标记
                    classWrapper.accessFlags = classWrapper.accessFlags and AccessFlags.FINAL.value.inv()
                    dexPool.internClass(classWrapper)
                } else {
                    dexPool.internClass(classDef)
                }
            }
            val newDexFile = File(unZipFile.parent, originalDexFile.name.getBaseName() + "_tmp.dex")
            dexPool.writeTo(FileDataStore(newDexFile));

            // 替换DEX
            originalDexFile.delete()
            originalDexFile.writeBytes(newDexFile.readBytes())
        }
    }

    /**
     *  让 com.ks.XpApplication 继承宿主的 Application
     */
    private fun changeMyApplicationSuper(applicationDexFile: AppDex) {
        val dexBackedDexFile =
                DexFileFactory.loadDexFile(appendDexFile, Opcodes.getDefault())
        val dexWriter = DexRewriter(object : RewriterModule() {
            override fun getClassDefRewriter(rewriters: Rewriters): Rewriter<ClassDef> {
                return Rewriter {
                    if (it.type.endsWith("${AndroidManifestEditorTask.PROXY_APPLICATION_NAME.replace(".", "/")};")) {
                        // 自己的application
                        val changeClassDef = ClassDefWrapper(it)
                        // 让我的 application 继承宿主的application
                        changeClassDef.setSupperClass(applicationDexFile.appClassDef.type)

                        // 因为改了父类 因此默认的构造方法 调用的 super 方法也要改
                        // 1. 查找默认无参构造方法
                        var defaultInitMethod: Method? = null
                        for (method in changeClassDef.originDirectMethods) {
                            if (method.parameters.size == 0 && method.name == "<init>" && method.returnType == "V") {
                                defaultInitMethod = method
                            }
                        }
                        if (defaultInitMethod != null) {
                            // 构建新的构造方法
                            val newInitMethod = changeInitSuperMethod(changeClassDef.type, defaultInitMethod, changeClassDef.superclass)
                            // 把原来的从集合中删除掉
                            changeClassDef.originDirectMethods.remove(defaultInitMethod)
                            // 在增加新的
                            changeClassDef.originDirectMethods.add(newInitMethod)
                        }
                        changeClassDef
                    } else {
                        it
                    }
                }
            }
        })
        // 修改完我dex
        val dexFile = dexWriter.dexFileRewriter.rewrite(dexBackedDexFile)

        // 替换DEX
        appendDexFile.delete()
        DexFileFactory.writeDexFile(appendDexFile.absolutePath, dexFile)

    }

    private fun classToSmali(classDef: ClassDef): String {
        val options = BaksmaliOptions()
        options.deodex = false
        options.implicitReferences = false
        options.parameterRegisters = true
        options.localsDirective = true
        options.sequentialLabels = true
        options.debugInfo = true
        options.codeOffsets = false
        options.accessorComments = false
        options.registerInfo = 0
        options.inlineResolver = null

        val classDefinition =
                ClassDefinition(options, classDef)
        val stringWriter = StringWriter()
        val indentingWriter = BaksmaliWriter(stringWriter)
        classDefinition.writeTo(indentingWriter)
        return stringWriter.toString()
    }

    private data class AppDex(val path: String, val dexFile: DexFile, val appClassDef: ClassDef)

    private suspend fun findApplicationDexFile(): AppDex? {
        // 寻找  Application 的 Dex
        val job = GlobalScope.async {
            //  过滤 dex 文件
            val dexs = unZipFile.listFiles { dir, name -> name.endsWith(".dex") }
            val jobs = mutableListOf<Deferred<AppDex?>>()
            // 多线程查找
            for (dex in dexs) {
                val childJob = async(Dispatchers.IO) {
                    val dexFile = openDex(dex.absolutePath)
                    // 查找 applicationName 在那个勒种
                    val appClassDef = dexFile.classes.stream().filter {
                        return@filter it.type.endsWith("${applicationName.replace(".", "/")};")
                    }.findFirst().orElseGet { null }
                    // 如果找到了 返回当前dex路径
                    return@async if (appClassDef != null) {
                        AppDex(dex.absolutePath, dexFile, appClassDef)
                    } else {
                        null
                    }
                }
                jobs.add(childJob)
            }
            return@async jobs.awaitAll().stream().filter { it != null }.findFirst().orElseGet { null }
        }
        return job.await()
    }

    override fun complete(result: File) {
        Log.d("ModifyApplicationDexTask", "找到Application的Dex  ${result.absolutePath}")
    }

    private fun openDex(path: String): DexFile {
        val dexBackedDexFile =
                DexFileFactory.loadDexFile(File(path), Opcodes.getDefault())
        val dexWriter = DexRewriter(object : RewriterModule() {})
        return dexWriter.dexFileRewriter.rewrite(dexBackedDexFile)
    }

}