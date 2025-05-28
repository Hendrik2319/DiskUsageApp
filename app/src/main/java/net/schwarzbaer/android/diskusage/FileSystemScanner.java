package net.schwarzbaer.android.diskusage;

public class FileSystemScanner {
    private static FileSystemScanner instance = null;
    private String scanLog = "<nothing scanned>";
    private boolean wasScanned = false;

    public static FileSystemScanner getInstance() {
        if (instance == null)
            instance = new FileSystemScanner();
        return instance;
    }

    public void scan(TextViewWriter textViewWriter) {
        // TODO
    }

    public String getScanLog() {
        return scanLog;
    }

    public boolean wasScanned() {
        return wasScanned;
    }
}
