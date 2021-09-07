package util;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static util.FileUtils.createOrExistsFile;
import static util.FileUtils.isSpace;

public class ApkZipUtils {
    private final static String[] APK_STANDARD_ALL_FILENAMES = new String[] {
            "classes.dex", "AndroidManifest.xml", "resources.arsc", "res", "r", "R",
            "lib", "libs", "assets", "META-INF", "kotlin" };
    private final static Pattern NO_COMPRESS_PATTERN = Pattern.compile("(" +
            "jpg|jpeg|png|gif|wav|mp2|mp3|ogg|aac|mpg|mpeg|mid|midi|smf|jet|rtttl|imy|xmf|mp4|" +
            "m4a|m4v|3gp|3gpp|3g2|3gpp2|amr|awb|wma|wmv|webm|webp|mkv)$");

    private static final int BUFFER_LEN = 8192;

    public static boolean zipFiles(final Collection<File> srcFiles,
                                   final File zipFile,
                                   ZipOutputStream zos,
                                   final String comment, Collection<String> uncompressedFilesOrExts)
            throws IOException {
        if (srcFiles == null || zipFile == null) return false;
        try {
            for (File srcFile : srcFiles) {
                if (!zipFile(srcFile, "", zos, comment,uncompressedFilesOrExts)) return false;
            }
            return true;
        } finally {
            if (zos != null) {
                zos.finish();
                zos.close();
            }
        }
    }

    public static boolean zipFile(final File srcFile,
                                  String rootPath,
                                  final ZipOutputStream zos,
                                  final String comment, Collection<String> uncompressedFilesOrExts)
            throws IOException {
        rootPath = rootPath + (isSpace(rootPath) ? "" : File.separator) + srcFile.getName();
        if (srcFile.isDirectory()) {
            File[] fileList = srcFile.listFiles();
            if (fileList == null || fileList.length <= 0) {
                ZipEntry entry = new ZipEntry(rootPath + '/');
                entry.setComment(comment);
                zos.putNextEntry(entry);
                zos.closeEntry();
            } else {
                for (File file : fileList) {
                    if (!zipFile(file, rootPath, zos, comment, uncompressedFilesOrExts)) return false;
                }
            }
        } else {
            InputStream is = null;
            try {
                is = new BufferedInputStream(new FileInputStream(srcFile));
                ZipEntry entry = new ZipEntry(rootPath);

                final String extension = FileUtils.getFileExtension(srcFile);
                if (uncompressedFilesOrExts.contains(extension) || uncompressedFilesOrExts.contains(rootPath)) {
                    entry.setMethod(ZipEntry.STORED);
                    entry.setSize(srcFile.length());
                    BufferedInputStream unknownFile = new BufferedInputStream(new FileInputStream(srcFile));
                    CRC32 crc = calculateCrc(unknownFile);
                    entry.setCrc(crc.getValue());
                    unknownFile.close();
                } else {
                    entry.setMethod(ZipEntry.DEFLATED);
                }
                entry.setComment(comment);
                zos.putNextEntry(entry);
                byte buffer[] = new byte[BUFFER_LEN];
                int len;
                while ((len = is.read(buffer, 0, BUFFER_LEN)) != -1) {
                    zos.write(buffer, 0, len);
                }
                zos.closeEntry();
            } finally {
                if (is != null) {
                    is.close();
                }
            }
        }
        return true;
    }

    public static CRC32 calculateCrc(InputStream input) throws IOException {
        CRC32 crc = new CRC32();
        int bytesRead;
        byte[] buffer = new byte[8192];
        while((bytesRead = input.read(buffer)) != -1) {
            crc.update(buffer, 0, bytesRead);
        }
        return crc;
    }

    public static List<File> unzipFile(final File zipFile,
                                       final File destDir,
                                       Collection<String> uncompressedFilesOrExts,
                                       final String keyword)
            throws IOException {
        if (zipFile == null || destDir == null) return null;
        List<File> files = new ArrayList<>();
        ZipFile zip = new ZipFile(zipFile);
        Enumeration<?> entries = zip.entries();
        try {
            if (isSpace(keyword)) {
                while (entries.hasMoreElements()) {
                    ZipEntry entry = ((ZipEntry) entries.nextElement());
                    String entryName = entry.getName().replace("\\", "/");
                    if (entryName.contains("../")) {
                        Log.e("ZipUtils", "entryName: " + entryName + " is dangerous!");
                        continue;
                    }
                    if (!unzipChildFile(destDir, files, zip, entry, entryName, uncompressedFilesOrExts)) {
                        return files;
                    }
                }
            } else {
                while (entries.hasMoreElements()) {
                    ZipEntry entry = ((ZipEntry) entries.nextElement());
                    String entryName = entry.getName().replace("\\", "/");
                    if (entryName.contains("../")) {
                        Log.e("ZipUtils", "entryName: " + entryName + " is dangerous!");
                        continue;
                    }
                    if (entryName.contains(keyword)) {
                        if (!unzipChildFile(destDir, files, zip, entry, entryName, uncompressedFilesOrExts))
                            return files;
                    }
                }
            }
        } finally {
            zip.close();
        }
        return files;
    }


    private static boolean unzipChildFile(final File destDir,
                                          final List<File> files,
                                          final ZipFile zip,
                                          final ZipEntry entry,
                                          final String name,
                                          Collection<String> uncompressedFilesOrExts) throws IOException {
        File file = new File(destDir, name);
        files.add(file);
        if (entry.isDirectory()) {
            return createOrExistsDir(file);
        } else {
            if (!createOrExistsFile(file)) return false;
            InputStream in = null;
            OutputStream out = null;
            try {
                if (isAPKFileNames(name) && entry.getMethod() == 0) {
                    String ext = "";
                    if (entry.getSize() != 0) {
                        ext = FileUtils.getFileExtension(file);
                    }

                    if (ext.isEmpty() || !NO_COMPRESS_PATTERN.matcher(ext).find()) {
                        ext = name;
                    }
                    if (!uncompressedFilesOrExts.contains(ext)) {
                        uncompressedFilesOrExts.add(name);
                    }
                }

                in = new BufferedInputStream(zip.getInputStream(entry));
                out = new BufferedOutputStream(new FileOutputStream(file));
                byte buffer[] = new byte[BUFFER_LEN];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
            } finally {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            }
        }
        return true;
    }


    public static boolean createOrExistsDir(final File file) {
        return file != null && (file.exists() ? file.isDirectory() : file.mkdirs());
    }

    private static boolean isAPKFileNames(String file) {
        for (String apkFile : APK_STANDARD_ALL_FILENAMES) {
            if (apkFile.equals(file) || file.startsWith(apkFile + "/")) {
                return true;
            }
        }
        return false;
    }

}
