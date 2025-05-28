package net.schwarzbaer.android.diskusage.models;

import net.schwarzbaer.android.diskusage.views.TextViewWriter;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileSystemScanner {
    private static FileSystemScanner instance = null;

    public static FileSystemScanner getInstance() {
        if (instance == null)
            instance = new FileSystemScanner();
        return instance;
    }

    private String scanLog = "<nothing scanned>";
    private boolean wasScanned = false;
    private Storage[] storages = null;

    public static boolean acceptSubFolder(File file) {
        if (file==null) return false;
        String name = file.getName();
        return file.isDirectory() && !name.equals(".") && !name.equals("..");
    }
    public static boolean acceptSubFolderNoSym(File file) {
        return acceptSubFolder(file) && !isSymbolicLink(file);
    }
    public static boolean acceptFileNoSym(File file) {
        if (file==null) return false;
        return file.isFile() && !isSymbolicLink(file);
    }

    private static boolean isSymbolicLink(File file) {
        Path path;
        try { path = file.toPath(); }
        catch (Exception e) { return false; }
        return Files.isSymbolicLink(path);
    }

    public void scan(TextViewWriter textViewWriter) {
        storages = determineStorages(textViewWriter);

        if (storages!=null) {
            for (int i=0; i<storages.length; i++) {
                Storage storage = storages[i];
                textViewWriter.addLine("[%d/%d] Storage \"%s\"", i+1, storages.length, storage.getPath());
                storage.scanFolder(textViewWriter);
            }
        }

        scanLog = textViewWriter.getCompleteText();
        wasScanned = true;
    }

    private Storage[] determineStorages(TextViewWriter textViewWriter) {
        File root = new File("/storage");

        if (!root.isDirectory()) {
            setStoragesErrorMsg_RootIsNoDir(textViewWriter, root);
            return null;
        }

        File[] storageFolders = root.listFiles(FileSystemScanner::acceptSubFolder);

        if (storageFolders==null) {
            File standardStorage = new File(root,"emulated");
            if (!standardStorage.isDirectory()) {
                setStoragesErrorMsg_StandardStorageIsNoDir(textViewWriter, root, standardStorage);
                return null;
            }

            File[] files = standardStorage.listFiles(FileSystemScanner::acceptSubFolder);
            if (files!=null) {
                return getStoragesFromStandardStorage(textViewWriter, standardStorage);
            }

            standardStorage = new File(standardStorage, "0");
            if (!standardStorage.isDirectory()) {
                setStoragesErrorMsg_StandardStorageIsNoDir(textViewWriter, root, standardStorage);
                return null;
            }

            files = standardStorage.listFiles(FileSystemScanner::acceptSubFolder);
            if (files!=null) {
                return getStoragesFromStandardStorage(textViewWriter, standardStorage);
            }

            setStoragesErrorMsg_CantReadStandardStorage(textViewWriter, root, standardStorage);
            return null;
        }

        for (int i=0; i<storageFolders.length; i++) {
            File storageFolder = storageFolders[i];
            if (storageFolder.getName().equals("emulated")) {
                File[] content = storageFolder.listFiles();
                if (content==null) {
                    File folder = new File(storageFolders[i], "0");
                    if (folder.isDirectory())
                        storageFolders[i] = folder;
                }
            }
        }

        return getStoragesFromFolderList(textViewWriter, storageFolders);
    }

    private Storage[] getStoragesFromFolderList(TextViewWriter textViewWriter, File[] storageFolders) {
        textViewWriter.setText("%d storages found", storageFolders.length);

        Storage[] storages = new Storage[storageFolders.length];
        for (int i=0; i<storageFolders.length; i++ ) {
            File folder = storageFolders[i];
            textViewWriter.addLine("   [%d] folder: \"%s\"", i+1, folder.getAbsolutePath());
            storages[i] = new Storage(folder);
        }

        return storages;
    }

    private Storage[] getStoragesFromStandardStorage(TextViewWriter textViewWriter, File standardStorage) {
        textViewWriter.setText("1 storages found");
        textViewWriter.addLine("   Standard storage folder: \"%s\"", standardStorage.getAbsolutePath());

        return new Storage[]{ new Storage(standardStorage) };
    }

    private void setStoragesErrorMsg_CantReadStandardStorage(TextViewWriter textViewWriter, File root, File standardStorage) {
        textViewWriter.setText("Can't read storages.");
        if (!root.canRead())
            textViewWriter.addLine("   Have no read permission for folder \"%s\".", root.getAbsolutePath());
        if (!standardStorage.canRead())
            textViewWriter.addLine("   Have no read permission for standard storage folder \"%s\".", standardStorage.getAbsolutePath());
    }

    private void setStoragesErrorMsg_StandardStorageIsNoDir(TextViewWriter textViewWriter, File root, File standardStorage) {
        textViewWriter.setText("Can't read storages.");
        if (!root.canRead())
            textViewWriter.addLine("   Have no read permission for folder \"%s\".", root.getAbsolutePath());
        if (!standardStorage.exists())
            textViewWriter.addLine("   Standard storage folder \"%s\" doesn't exist.", standardStorage.getAbsolutePath());
        else
            textViewWriter.addLine("   File entry for standard storage folder \"%s\" exists, but isn't a folder.", standardStorage.getAbsolutePath());
    }

    private void setStoragesErrorMsg_RootIsNoDir(TextViewWriter textViewWriter, File root) {
        textViewWriter.setText("Can't read storages.");
        if (!root.exists())
            textViewWriter.addLine("   Folder \"%s\" doesn't exist.", root.getAbsolutePath());
        else
            textViewWriter.addLine("   File entry \"%s\" exists, but isn't a folder.", root.getAbsolutePath());
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

    public Storage getStorage(int index)
    {
        if (storages==null)
            return null;

        if (index<0 || index>=storages.length)
            return null;

        return storages[index];
    }
}
