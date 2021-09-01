package util

const val IS_DEBUG = true

object Log {
    @JvmStatic
    fun d(tag: String, msg: String) {
        println("$tag : $msg")
    }

    @JvmStatic
    fun e(tag: String, msg: String): Unit {
        println("$tag : $msg")
    }
}