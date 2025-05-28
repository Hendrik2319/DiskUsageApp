package net.schwarzbaer.android.diskusage;

import org.jetbrains.annotations.NotNull;

import java.io.File;

enum FileCategory {
    Images("Images", "bmp", "jpg", "jpeg", "png", "tga", "gif", "tif"),
    Videos("Videos", "mp4", "mpg", "mpeg", "avi", "ogv", "wmv"),
    Audios("Audio Files", "mp3", "wav", "opus", "ogg", "oga", "wma"),
    Docs("Documents", "txt", "pdf", "rtf", "htm", "html", "odt", "ods", "doc", "docx", "docm", "dot", "dotx", "dotm", "xls", "xlsx", "xlsm", "ppt", "pptx", "pptm"),
    Compressed("Compressed Archives", "zip", "7z", "rar", "arj", "tar", "gz"),
    APK("Installation Files (APK)", "apk"),
    Other("Others");
    private final String label;
    private final String[] extensions;

    FileCategory(@NotNull String label, String... extensions) {
        this.label = label;
        for (int i = 0; i < extensions.length; i++)
            extensions[i] = extensions[i].toLowerCase();
        this.extensions = extensions;
    }

    boolean isFileInCategory(File file) {
        if (file == null) return false;
        if (file.isFile()) return false;

        String name = file.getName();

        int pos = name.lastIndexOf('.');
        if (pos < 0) return false;

        String ext = name.substring(pos + 1).toLowerCase();
        for (String catExt : extensions)
            if (ext.equals(catExt))
                return true;

        return false;
    }
}
