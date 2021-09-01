package task


abstract class Task<IN, OUT>() {
    fun call(): OUT {
        val result = execute()
        complete(result)
        return result
    }

    abstract fun execute(): OUT

    abstract fun complete(result: OUT)

}
