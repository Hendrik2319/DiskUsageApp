package net.schwarzbaer.android.diskusage;

import android.os.StatFs;

import java.io.File;
import java.util.Locale;

public class Storage {

    private final File folder;
    private final String comment;

    public Storage(File folder) {
        this.folder = folder;
        comment = getStorageStats(folder);
    }

    public String getPath() {
        return folder.getAbsolutePath();
    }

    public String getComment() {
        return comment;
    }

    public boolean hasFiles() {
        return false; // TODO
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
}
