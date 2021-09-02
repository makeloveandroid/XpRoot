package util;

public class TextUtils {

    public static boolean isEmpty(String anyPathInFs) {
        return anyPathInFs == null || anyPathInFs.length() <= 0;
    }
}
