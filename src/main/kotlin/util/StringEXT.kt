package util


fun String.getBaseName(): String {
    val x = lastIndexOf('.')
    return if (x >= 0) substring(0, x) else this
}

fun String.getExtension(): String? {
    val index: Int = lastIndexOf('.')
    return substring(index + 1)
}
