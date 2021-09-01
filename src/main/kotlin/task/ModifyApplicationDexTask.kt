package task

import ClassDefWrapper
import kotlinx.coroutines.*
import org.jf.baksmali.Adaptors.ClassDefinition
import org.jf.baksmali.BaksmaliOptions
import org.jf.baksmali.formatter.BaksmaliWriter
import org.jf.dexlib2.DexFileFactory
import org.jf.dexlib2.Opcodes
import org.jf.dexlib2.iface.ClassDef
import org.jf.dexlib2.iface.DexFile
import org.jf.dexlib2.rewriter.DexRewriter
import org.jf.dexlib2.rewriter.RewriterModule
import org.jf.dexlib2.writer.io.FileDataStore
import org.jf.dexlib2.writer.pool.DexPool
import util.Log
import util.getBaseName
import java.io.File
import java.io.StringWriter
import java.lang.RuntimeException

class ModifyApplicationDexTask(val unZipFile: File, val applicationName: String) : Task<File, File>() {
    override fun execute(): File = runBlocking<File> {
        val applicationDexFile = findApplicationDexFile() ?: throw RuntimeException("没有找到 存在 application 的dex")
        // 注入方法
        return@runBlocking injectAppStaticMethod(applicationDexFile)
    }

    private fun injectAppStaticMethod(applicationDexFile: AppDex): File {
        val appClassDef = applicationDexFile.appClassDef
        val dexFile = applicationDexFile.dexFile
        val originalDexFile = File(applicationDexFile.path)
        val dexPool = DexPool(dexFile.opcodes)
        // 写入new dex
        for (classDef in dexFile.classes) {
            if (classDef == appClassDef) {
                /**
                 * 为 Application 注入方法 start
                 *    static {
                 *     XpRoot.start();
                 *    }
                 */
                val staticMethod = classDef.directMethods.filter {
                    it.name == "<clinit>"
                }.firstOrNull()
                val classWrapper = ClassDefWrapper(appClassDef)
                if (staticMethod == null) {
                    // 没有static 方法 加入一个
                    classWrapper.originDirectMethods.add(InjectMethodBuilder.buildStaticContextMethod(appClassDef.type))
                } else {
                    // 修改staticMethod
                    // 先把原来的方法删除掉
                    classWrapper.originDirectMethods.remove(staticMethod)
                    val newMethod = InjectMethodBuilder.buildStaticContextMethod(appClassDef.type, staticMethod)
                    classWrapper.originDirectMethods.add(newMethod)
                }

//                println("\n${classToSmali(classWrapper)}")

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

        return newDexFile
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