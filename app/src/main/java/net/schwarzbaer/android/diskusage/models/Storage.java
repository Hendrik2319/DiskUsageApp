package net.schwarzbaer.android.diskusage.models;

import android.os.StatFs;

import net.schwarzbaer.android.diskusage.views.TextViewWriter;

import java.io.File;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;
import java.util.function.Function;

public class Storage {

    private final File folder;
    private final String comment;
    private final EnumMap<FileCategory, ScannedFolder> categoryRoots;

    public Storage(File folder) {
        this.folder = folder.getAbsoluteFile();
        comment = getStorageStats(folder);
        categoryRoots = new EnumMap<>(FileCategory.class);
    }

    public String getPath() {
        return folder.getAbsolutePath();
    }

    public String getComment() {
        return comment;
    }

    public boolean hasFiles() {
        return !categoryRoots.isEmpty();
    }

    public long getFileCount(FileCategory fileCat)
    {
        ScannedFolder scannedFolder = categoryRoots.get(fileCat);
        return scannedFolder == null ? 0 : scannedFolder.getTotalFileCount();
    }

    public long getFileSize_Byte(FileCategory fileCat)
    {
        ScannedFolder scannedFolder = categoryRoots.get(fileCat);
        return scannedFolder == null ? 0 : scannedFolder.getTotalSize_Byte();
    }

    private static String getStorageStats(File folder) {
        try {
            StatFs statFs = new StatFs(folder.getAbsolutePath());

            long blockSize, totalBlocks, availableBlocks;

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
                blockSize = statFs.getBlockSizeLong();
                totalBlocks = statFs.getBlockCountLong();
                availableBlocks = statFs.getAvailableBlocksLong();
            } else {
                blockSize = statFs.getBlockSize();
                totalBlocks = statFs.getBlockCount();
                availableBlocks = statFs.getAvailableBlocks();
            }

            long totalSize = totalBlocks * blockSize;
            long availableSize = availableBlocks * blockSize;
            long usedSize = totalSize - availableSize;

            return String.format(
                    Locale.ENGLISH,
                    "total: %d MB%nfree: %d MB%nused: %d MB",
                    totalSize / (1 << 20),
                    availableSize / (1 << 20),
                    usedSize / (1 << 20)
            );
        } catch (Exception ex) {
            return String.format("Can't get usage.%nGot an Exception[ %s ]: \"%s\"",
                    ex.getClass().getName(), ex.getMessage()
            );
        }
    }

    public void scanFolder(TextViewWriter textViewWriter) {
        scanFolder(
                folder,
                fileCategory -> categoryRoots.computeIfAbsent(fileCategory, cat -> new ScannedFolder(cat, folder)),
                textViewWriter
        );
        categoryRoots.forEach((fileCat, scannedFolder) -> {
            scannedFolder.getTotalFileCount();
            scannedFolder.getTotalSize_Byte();
        });
    }

    private static void scanFolder(File folder, Function<FileCategory,ScannedFolder> getParentScannedFolder, TextViewWriter textViewWriter) {
        Map<FileCategory, ScannedFolder> knownFolders = new EnumMap<>(FileCategory.class);

        File[] files = folder.listFiles(FileSystemScanner::acceptFileNoSym);
        if (files!=null) {
            for (File file : files) {
                boolean typeFound = false;
                for (FileCategory fileCat : FileCategory.values()) {
                    if (fileCat==FileCategory.Other) continue;
                    if (fileCat.isFileInCategory(file)) {
                        getScannedFolder(knownFolders, fileCat, getParentScannedFolder).addFile(file);
                        typeFound = true;
                    }
                }
                if (!typeFound)
                    getScannedFolder(knownFolders, FileCategory.Other, getParentScannedFolder).addFile(file);
            }
            if (textViewWriter!=null)
                textViewWriter.addLine("    %d Files added", files.length);
        }

        File[] subFolders = folder.listFiles(FileSystemScanner::acceptSubFolderNoSym);
        if (subFolders!=null) {
            for (int i=0; i<subFolders.length; i++) {
                File subFolder = subFolders[i];
                if (textViewWriter!=null)
                    textViewWriter.addLine("    [%d/%d] SubFolder \"%s\"", i+1, subFolders.length, subFolder.getName());
                scanFolder(subFolder, fileCat -> getScannedFolder(knownFolders, fileCat, getParentScannedFolder).addFolder(subFolder), null);
            }
        }

        if (files==null && subFolders==null && textViewWriter!=null)
            textViewWriter.addLine("    Can't read folder content.");
    }

    private static ScannedFolder getScannedFolder(
            Map<FileCategory, ScannedFolder> knownFolders,
            FileCategory fileCat,
            Function<FileCategory, ScannedFolder> getParentScannedFolder
    ) {
        return knownFolders.computeIfAbsent(fileCat, getParentScannedFolder);
    }

    public static class ScannedFolder
    {

        private final FileCategory fileCategory;
        private final File folder;
        private final List<File> localFiles;
        private final List<ScannedFolder> subfolders;
        private Long totalFileCount;
        private Long totalSize_Byte;

        public ScannedFolder(FileCategory fileCategory, File folder)
        {
            this.fileCategory = fileCategory;
            this.folder = folder;
            localFiles = new Vector<>();
            subfolders = new Vector<>();
            totalFileCount = null;
            totalSize_Byte = null;
        }

        public void addFile(File file)
        {
            localFiles.add(file);
        }

        public ScannedFolder addFolder(File subFolder)
        {
            ScannedFolder scannedFolder = new ScannedFolder(fileCategory, subFolder);
            subfolders.add(scannedFolder);
            return scannedFolder;
        }

        public long getTotalFileCount()
        {
            if (totalFileCount==null)
            {
                totalFileCount = (long) localFiles.size();
                subfolders.forEach(subfolder -> totalFileCount += subfolder.getTotalFileCount());
            }

            return totalFileCount;
        }

        public long getTotalSize_Byte()
        {
            if (totalSize_Byte==null)
            {
                totalSize_Byte = 0L;
                localFiles.forEach(file -> totalSize_Byte += file.length());
                subfolders.forEach(subfolder -> totalSize_Byte += subfolder.getTotalSize_Byte());
            }

            return totalSize_Byte;
        }
    }
}
