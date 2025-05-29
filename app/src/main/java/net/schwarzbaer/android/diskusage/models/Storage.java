package net.schwarzbaer.android.diskusage.models;

import android.os.StatFs;

import androidx.annotation.NonNull;

import net.schwarzbaer.android.diskusage.views.UiThreadSafeTextViewWriter;

import java.io.File;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;
import java.util.function.Function;

public class Storage
{
    public enum SortOrder
    {
        Original("Original Order"),
        BySizeAsc("by Size ˄"),
        BySizeDesc("by Size ˅"),
        ByNameAsc("by Name ˄"),
        ByNameDesc("by Name ˅"),
        ;
        private final String label;

        SortOrder(@NonNull String label)
        {
            this.label = label;
        }

        public static SortOrder valueOf_checked(String str)
        {
            try {
                return SortOrder.valueOf(str);
            } catch (Exception e) {
                return null;
            }
        }

        @NonNull
        @Override
        public String toString()
        {
            return label;
        }
    }

    public static final int FolderID_Root = -1;
    private static final Comparator<String> nameComp = Comparator
            .<String,String>comparing(String::toLowerCase)
            .thenComparing(Comparator.naturalOrder());

    private final File storageFolder;
    private final String comment;
    private final Map<FileCategory, ScannedFolder> categoryRoots;
    private final List<ScannedFolder> scannedFolderList;

    public Storage(File storageFolder) {
        this.storageFolder = storageFolder.getAbsoluteFile();
        comment = getStorageStats(storageFolder);
        categoryRoots = new EnumMap<>(FileCategory.class);
        scannedFolderList = new Vector<>();
    }

    public String getPath() {
        return storageFolder.getAbsolutePath();
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
                    "total: %s%nfree: %s%nused: %s",
                    getFormatedSize( totalSize ),
                    getFormatedSize( availableSize ),
                    getFormatedSize( usedSize )
            );
        } catch (Exception ex) {
            return String.format("Can't get usage.%nGot an Exception[ %s ]: \"%s\"",
                    ex.getClass().getName(), ex.getMessage()
            );
        }
    }

    public void scanFolder(UiThreadSafeTextViewWriter textViewWriter) {
        scanFolder(
                storageFolder,
                fileCategory -> categoryRoots.computeIfAbsent(fileCategory, cat -> createScannedFolder(cat, storageFolder)),
                textViewWriter
        );
        categoryRoots.forEach((fileCat, scannedFolder) -> {
            scannedFolder.getTotalFileCount();
            scannedFolder.getTotalSize_Byte();
        });
    }

    private static void scanFolder(File folder, Function<FileCategory,ScannedFolder> getParentScannedFolder, UiThreadSafeTextViewWriter textViewWriter) {
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
            if (textViewWriter!=null) {
                if (files.length == 1)
                    textViewWriter.addLine("    1 File added");
                else
                    textViewWriter.addLine("    %d Files added", files.length);
            }
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

    public ScannedFolder getRootFolder(FileCategory fileCat)
    {
        return categoryRoots.get(fileCat);
    }

    public ScannedFolder getFolder(int folderID)
    {
        if (folderID < 0 || folderID >= scannedFolderList.size()) return null;
        return scannedFolderList.get(folderID);
    }

    private ScannedFolder createScannedFolder(FileCategory fileCategory, File subFolder)
    {
        int folderID = scannedFolderList.size();
        ScannedFolder scannedFolder = new ScannedFolder(fileCategory, subFolder, folderID);
        scannedFolderList.add(scannedFolder);
        return scannedFolder;
    }

    public static String getFormatedSize(long size_Byte)
    {
        double value = size_Byte;
        if (value < 1200) return String.format(Locale.ENGLISH, "%d B", size_Byte);

        value /= 1024;
        if (value < 10  ) return String.format(Locale.ENGLISH, "%1.2f kB", value);
        if (value < 100 ) return String.format(Locale.ENGLISH, "%1.1f kB", value);
        if (value < 1000) return String.format(Locale.ENGLISH, "%1.0f kB", value);

        value /= 1024;
        if (value < 10  ) return String.format(Locale.ENGLISH, "%1.2f MB", value);
        if (value < 100 ) return String.format(Locale.ENGLISH, "%1.1f MB", value);
        if (value < 1000) return String.format(Locale.ENGLISH, "%1.0f MB", value);

        value /= 1024;
        if (value < 10  ) return String.format(Locale.ENGLISH, "%1.2f GB", value);
        if (value < 100 ) return String.format(Locale.ENGLISH, "%1.1f GB", value);
        if (value < 1000) return String.format(Locale.ENGLISH, "%1.0f GB", value);

        value /= 1024;
        if (value < 10  ) return String.format(Locale.ENGLISH, "%1.2f TB", value);
        if (value < 100 ) return String.format(Locale.ENGLISH, "%1.1f TB", value);
        return                   String.format(Locale.ENGLISH, "%1.0f TB", value);
    }

    public class ScannedFolder
    {
        private final FileCategory fileCategory;
        private final File folder;
        private final int folderID;
        private final List<File> localFiles;
        private final List<ScannedFolder> subfolders;
        private Long totalFileCount;
        private Long totalSize_Byte;
        private Long localFilesSize_Byte;

        public ScannedFolder(@NonNull FileCategory fileCategory, @NonNull File folder, int folderID)
        {
            this.fileCategory = fileCategory;
            this.folder = folder;
            this.folderID = folderID;
            localFiles = new Vector<>();
            subfolders = new Vector<>();
            totalFileCount = null;
            totalSize_Byte = null;
            localFilesSize_Byte = null;
        }

        public void addFile(File file)
        {
            localFiles.add(file);
        }

        public ScannedFolder addFolder(File subFolder)
        {
            ScannedFolder scannedFolder = createScannedFolder(fileCategory, subFolder);
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

        public long getLocalFilesSize_Byte()
        {
            if (localFilesSize_Byte==null)
            {
                localFilesSize_Byte = 0L;
                localFiles.forEach(file -> localFilesSize_Byte += file.length());
            }

            return localFilesSize_Byte;
        }

        public long getTotalSize_Byte()
        {
            if (totalSize_Byte==null)
            {
                totalSize_Byte = getLocalFilesSize_Byte();
                subfolders.forEach(subfolder -> totalSize_Byte += subfolder.getTotalSize_Byte());
            }

            return totalSize_Byte;
        }

        public String getName()
        {
            return folder.getName();
        }

        public String getPath()
        {
            return folder.getAbsolutePath();
        }

        public int getLocalFileCount()
        {
            return localFiles.size();
        }

        public int getLocalFolderCount()
        {
            return subfolders.size();
        }

        public ScannedFolder getSubFolder(int index)
        {
            if (index < 0 || index >= subfolders.size()) return null;
            return subfolders.get(index);
        }

        public List<File> getLocalFiles(SortOrder sortOrder)
        {
            if (sortOrder==null) sortOrder = SortOrder.Original;
            Vector<File> vec = new Vector<>(localFiles);
            switch(sortOrder)
            {
                case Original: break;
                case BySizeAsc : vec.sort(Comparator.comparing(File::length)); break;
                case BySizeDesc: vec.sort(Comparator.comparing(File::length).reversed()); break;
                case ByNameAsc : vec.sort(Comparator.comparing(File::getName, nameComp)); break;
                case ByNameDesc: vec.sort(Comparator.comparing(File::getName, nameComp).reversed()); break;
            }
            return vec;
        }

        public int getFolderID()
        {
            return folderID;
        }
    }
}
