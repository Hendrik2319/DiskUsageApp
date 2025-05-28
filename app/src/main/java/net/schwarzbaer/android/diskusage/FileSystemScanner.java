package net.schwarzbaer.android.diskusage;

import java.io.File;
import java.util.Locale;

public class FileSystemScanner {
    private static FileSystemScanner instance = null;
    private String scanLog = "<nothing scanned>";
    private boolean wasScanned = false;
    private Storage[] storages = null;

    public static FileSystemScanner getInstance() {
        if (instance == null)
            instance = new FileSystemScanner();
        return instance;
    }

    private static boolean acceptStorageFolder(File file) {
        String name = file.getName();
        return file.isDirectory() && !name.equals(".") && !name.equals("..");
    }

    public void scan(TextViewWriter textViewWriter) {
        storages = determineStorages(textViewWriter);

        // TODO

        scanLog = textViewWriter.getCompleteText();
        wasScanned = true;
    }

    private Storage[] determineStorages(TextViewWriter textViewWriter) {
        File root = new File("/storage");

        if (!root.isDirectory()) {
            setStoragesErrorMsg_RootIsNoDir(textViewWriter, root);
            return null;
        }

        File[] storageFolders = root.listFiles(FileSystemScanner::acceptStorageFolder);

        if (storageFolders==null) {
            File standardStorage = new File(root,"emulated");
            if (!standardStorage.isDirectory()) {
                setStoragesErrorMsg_StandardStorageIsNoDir(textViewWriter, root, standardStorage);
                return null;
            }

            File[] files = standardStorage.listFiles(FileSystemScanner::acceptStorageFolder);
            if (files!=null) {
                return getStoragesFromStandardStorage(textViewWriter, standardStorage);
            }

            standardStorage = new File(standardStorage, "0");
            if (!standardStorage.isDirectory()) {
                setStoragesErrorMsg_StandardStorageIsNoDir(textViewWriter, root, standardStorage);
                return null;
            }

            files = standardStorage.listFiles(FileSystemScanner::acceptStorageFolder);
            if (files!=null) {
                return getStoragesFromStandardStorage(textViewWriter, standardStorage);
            }

            setStoragesErrorMsg_CantReadStandardStorage(textViewWriter, root, standardStorage);
            return null;
        }

        return getStoragesFromFolderList(textViewWriter, storageFolders);
    }

    private Storage[] getStoragesFromFolderList(TextViewWriter textViewWriter, File[] storageFolders) {
        textViewWriter.setText(String.format(Locale.ENGLISH, "%d storages found", storageFolders.length));

        Storage[] storages = new Storage[storageFolders.length];
        for (int i=0; i<storageFolders.length; i++ ) {
            File folder = storageFolders[i];
            textViewWriter.addLine(String.format(Locale.ENGLISH, "   [%d] folder: \"%s\"", i+1, folder.getAbsolutePath()));
            storages[i] = new Storage(folder);
        }

        return storages;
    }

    private Storage[] getStoragesFromStandardStorage(TextViewWriter textViewWriter, File standardStorage) {
        textViewWriter.setText("1 storages found");
        textViewWriter.addLine(String.format("   Standard storage folder: \"%s\"", standardStorage.getAbsolutePath()));

        return new Storage[]{ new Storage(standardStorage) };
    }

    private void setStoragesErrorMsg_CantReadStandardStorage(TextViewWriter textViewWriter, File root, File standardStorage) {
        textViewWriter.setText("Can't read storages.");
        if (!root.canRead())
            textViewWriter.addLine(String.format("   Have no read permission for folder \"%s\".", root.getAbsolutePath()));
        if (!standardStorage.canRead())
            textViewWriter.addLine(String.format("   Have no read permission for standard storage folder \"%s\".", standardStorage.getAbsolutePath()));
    }

    private void setStoragesErrorMsg_StandardStorageIsNoDir(TextViewWriter textViewWriter, File root, File standardStorage) {
        textViewWriter.setText("Can't read storages.");
        if (!root.canRead())
            textViewWriter.addLine(String.format("   Have no read permission for folder \"%s\".", root.getAbsolutePath()));
        if (!standardStorage.exists())
            textViewWriter.addLine(String.format("   Standard storage folder \"%s\" doesn't exist.", standardStorage.getAbsolutePath()));
        else
            textViewWriter.addLine(String.format("   File entry for standard storage folder \"%s\" exists, but isn't a folder.", standardStorage.getAbsolutePath()));
    }

    private void setStoragesErrorMsg_RootIsNoDir(TextViewWriter textViewWriter, File root) {
        textViewWriter.setText("Can't read storages.");
        if (!root.exists())
            textViewWriter.addLine(String.format("   Folder \"%s\" doesn't exist.", root.getAbsolutePath()));
        else
            textViewWriter.addLine(String.format("   File entry \"%s\" exists, but isn't a folder.", root.getAbsolutePath()));
    }

    public String getScanLog() {
        return scanLog;
    }

    public boolean wasScanned() {
        return wasScanned;
    }

    public boolean hasStorages() {
        return storages!=null;
    }

    public Storage[] getStorages() {
        return storages;
    }

}
