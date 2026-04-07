package catgirl.oneesama.data.controller;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;


import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import catgirl.oneesama.application.Application;
import catgirl.oneesama.data.model.chapter.ui.UiPage;
import catgirl.oneesama.data.settings.StorageSettings;

public class FileManager {

    public static String DOWNLOAD_FOLDER = "chapters";

    private static Map<Integer, Map<String, DocumentFile>> chapterFileCache = new ConcurrentHashMap<>();
    private static Map<Integer, DocumentFile> chapterDirCache = new ConcurrentHashMap<>();
    private static volatile DocumentFile downloadDirCache = null;

    public static void clearCache(int chapterId) {
        chapterFileCache.remove(chapterId);
        chapterDirCache.remove(chapterId);
    }

    public static void precacheChapter(Context context, int chapterId) {
        DocumentFile chapterDir = getChapterDirectory(context, chapterId);
        if (chapterDir == null) return;

        // If it's a regular file (internal storage), we don't need to iterative-cache it
        // because direct File I/O doesn't suffer from the same lookup penalties as SAF.
        if (chapterDir.getUri().getScheme().equals("file")) {
            return;
        }

        Map<String, DocumentFile> fileCache = new ConcurrentHashMap<>();

        for (DocumentFile file : chapterDir.listFiles()) {
            String name = file.getName();
            if (name != null) {
                fileCache.put(name, file);
            }
        }
        chapterFileCache.put(chapterId, fileCache);
    }

    private static StorageSettings getStorageSettings() {
        return Application.getApplicationComponent().getStorageSettingsProvider().retrieve();
    }

    public static DocumentFile getRootFolder(Context context) {
        StorageSettings settings = getStorageSettings();
        if (settings != null && settings.getTreeUri() != null) {
            return DocumentFile.fromTreeUri(context, Uri.parse(settings.getTreeUri()));
        }
        return null;
    }

    public static DocumentFile getDownloadDirectory(Context context) {
        return getDownloadDirectory(context, true);
    }

    public synchronized static DocumentFile getDownloadDirectory(Context context, boolean create) {
        if (downloadDirCache != null && downloadDirCache.exists() && DOWNLOAD_FOLDER.equals(downloadDirCache.getName())) {
            return downloadDirCache;
        }

        DocumentFile root = getRootFolder(context);
        if (root == null) return null;

        DocumentFile downloadDir = null;
        DocumentFile found = root.findFile(DOWNLOAD_FOLDER);
        if (found != null && DOWNLOAD_FOLDER.equals(found.getName())) {
            downloadDir = found;
        }

        // SAF Hack: sometimes findFile fails but it exists. Iterating listFiles can refresh it.
        if (downloadDir == null) {
            for (DocumentFile file : root.listFiles()) {
                if (DOWNLOAD_FOLDER.equals(file.getName())) {
                    downloadDir = file;
                    break;
                }
            }
        }

        if (create && (downloadDir == null || !downloadDir.isDirectory())) {
            downloadDir = root.createDirectory(DOWNLOAD_FOLDER);
            // If it created "chapters (1)", it means "chapters" exists but was invisible.
            // Try to recover by searching again now that the provider's cache might be refreshed.
            if (downloadDir != null && !DOWNLOAD_FOLDER.equals(downloadDir.getName())) {
                Log.w("FileManager", "Ghost folder detected during createDirectory: " + downloadDir.getName());
                for (DocumentFile file : root.listFiles()) {
                    if (DOWNLOAD_FOLDER.equals(file.getName())) {
                        downloadDir.delete(); // Delete the (1) version
                        downloadDir = file;   // Use the original
                        break;
                    }
                }
            }
        }

        if (downloadDir != null && DOWNLOAD_FOLDER.equals(downloadDir.getName())) {
            downloadDirCache = downloadDir;
        }
        return downloadDir;
    }

    public static DocumentFile getChapterDirectory(Context context, int chapterId) {
        return getChapterDirectory(context, chapterId, true);
    }

    public synchronized static DocumentFile getChapterDirectory(Context context, int chapterId, boolean create) {
        if (chapterDirCache.containsKey(chapterId)) {
            DocumentFile cached = chapterDirCache.get(chapterId);
            if (cached != null && cached.exists()) {
                return cached;
            }
        }

        DocumentFile downloadDir = getDownloadDirectory(context, create);
        if (downloadDir == null) return null;

        String idStr = String.valueOf(chapterId);
        DocumentFile chapterDir = null;
        DocumentFile foundChapter = downloadDir.findFile(idStr);
        if (foundChapter != null && idStr.equals(foundChapter.getName())) {
            chapterDir = foundChapter;
        }

        if (chapterDir == null) {
            for (DocumentFile file : downloadDir.listFiles()) {
                if (idStr.equals(file.getName())) {
                    chapterDir = file;
                    break;
                }
            }
        }

        if (create && (chapterDir == null || !chapterDir.isDirectory())) {
            chapterDir = downloadDir.createDirectory(idStr);
            // Same ghost recovery for chapter subfolders
            if (chapterDir != null && !idStr.equals(chapterDir.getName())) {
                Log.w("FileManager", "Ghost chapter folder detected: " + chapterDir.getName());
                for (DocumentFile file : downloadDir.listFiles()) {
                    if (idStr.equals(file.getName())) {
                        chapterDir.delete();
                        chapterDir = file;
                        break;
                    }
                }
            }
        }

        if (chapterDir != null && idStr.equals(chapterDir.getName())) {
            chapterDirCache.put(chapterId, chapterDir);
        }

        return chapterDir;
    }


    public static File getChapterFolder(int chapterId) {
        return getChapterFolder(chapterId, true);
    }

    public static File getChapterFolder(int chapterId, boolean create) {
        DocumentFile chapterDir = getChapterDirectory(Application.getContextOfApplication(), chapterId, create);
        if (chapterDir != null && chapterDir.getUri().getScheme().equals("file")) {
            return new File(chapterDir.getUri().getPath());
        }

        File folder = new File(Application.getContextOfApplication().getExternalFilesDir(null), DOWNLOAD_FOLDER + File.separator + chapterId);
        if(create && !folder.isDirectory() && !folder.mkdirs()) {
            Log.e("FileManager", "Could not create folder: " + folder.toString());
            return null;
        }
        return folder;
    }

    public synchronized static DocumentFile getPageDocumentFile(Context context, int chapterId, UiPage page) {
        String[] parts = page.getUrl().split("/");
        String fileName = parts[parts.length - 1];

        Map<String, DocumentFile> fileCache = chapterFileCache.get(chapterId);
        if (fileCache != null && fileCache.containsKey(fileName)) {
            DocumentFile cached = fileCache.get(fileName);
            if (cached != null && cached.exists()) {
                return cached;
            }
        }

        DocumentFile chapterDir = getChapterDirectory(context, chapterId);
        if (chapterDir == null) return null;

        DocumentFile file = chapterDir.findFile(fileName);
        if (file != null) {
            if (fileCache == null) {
                fileCache = new ConcurrentHashMap<>();
                chapterFileCache.put(chapterId, fileCache);
            }
            fileCache.put(fileName, file);
        }
        return file;
    }

    public static File getPageFile(int chapterId, UiPage page) {
        DocumentFile pageFile = getPageDocumentFile(Application.getContextOfApplication(), chapterId, page);
        if (pageFile != null && pageFile.getUri().getScheme().equals("file")) {
            return new File(pageFile.getUri().getPath());
        }

        String[] parts = page.getUrl().split("/");
        return new File(getChapterFolder(chapterId), parts[parts.length - 1]);
    }

    public static boolean fileExists(int chapterId, UiPage page) {
        DocumentFile pageFile = getPageDocumentFile(Application.getContextOfApplication(), chapterId, page);
        if (pageFile != null) return pageFile.exists();

        File folder = new File(Application.getContextOfApplication().getExternalFilesDir(null), DOWNLOAD_FOLDER + File.separator + chapterId);
        String[] parts = page.getUrl().split("/");
        return new File(folder, parts[parts.length - 1]).exists();
    }

    public static InputStream getInputStream(int chapterId, UiPage page) {
        DocumentFile pageFile = getPageDocumentFile(Application.getContextOfApplication(), chapterId, page);
        if (pageFile != null) {
            try {
                return Application.getContextOfApplication().getContentResolver().openInputStream(pageFile.getUri());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return null;
            }
        }

        try {
            return new BufferedInputStream(new FileInputStream(getPageFile(chapterId, page)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static File getCache(String cacheName) {
        return new File(Application.getContextOfApplication().getExternalFilesDir(null), cacheName);
    }

    public static boolean chapterFolderExists(int chapterId) {
        Context context = Application.getContextOfApplication();
        DocumentFile downloadDir = getDownloadDirectory(context, false);
        if (downloadDir != null) {
            String idStr = String.valueOf(chapterId);
            DocumentFile foundChapter = downloadDir.findFile(idStr);
            if (foundChapter != null && idStr.equals(foundChapter.getName())) {
                return true;
            }
            for (DocumentFile file : downloadDir.listFiles()) {
                if (idStr.equals(file.getName())) {
                    return true;
                }
            }
        }
        File folder = new File(Application.getContextOfApplication().getExternalFilesDir(null), DOWNLOAD_FOLDER + File.separator + chapterId);
        return folder.exists() && folder.isDirectory();
    }

    public static void deleteFolder(int chapterId) {
        clearCache(chapterId);
        Context context = Application.getContextOfApplication();

        // 1. Try SAF deletion - be VERY aggressive
        DocumentFile downloadDir = getDownloadDirectory(context, false);
        if (downloadDir != null) {
            String idStr = String.valueOf(chapterId);
            for (DocumentFile file : downloadDir.listFiles()) {
                String name = file.getName();
                if (name != null && (name.equals(idStr) || name.startsWith(idStr + " ("))) {
                    Log.d("FileManager", "Deleting SAF folder: " + name);
                    // Rename workaround to break ghosting
                    file.renameTo("del_" + System.currentTimeMillis() + "_" + name);
                    recursiveDelete(file);
                }
            }
        }

        // 2. Always clean up internal storage
        File folder = new File(context.getExternalFilesDir(null), DOWNLOAD_FOLDER + File.separator + chapterId);
        if (folder.exists()) {
            manualDeleteDirectory(folder);
        }
    }

    private static void manualDeleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    manualDeleteDirectory(file);
                } else {
                    if (!file.delete()) {
                        Log.e("FileManager", "Failed to delete file: " + file.getAbsolutePath());
                    }
                }
            }
        }
        if (!directory.delete()) {
            Log.e("FileManager", "Failed to delete directory: " + directory.getAbsolutePath());
        }
    }

    private static void recursiveDelete(DocumentFile file) {
        try {
            if (file.isDirectory()) {
                DocumentFile[] children = file.listFiles();
                if (children != null) {
                    for (DocumentFile child : children) {
                        recursiveDelete(child);
                    }
                }
            }
        } catch (Exception e) {
            Log.e("FileManager", "Error during recursive directory traversal: " + file.getUri(), e);
        }

        try {
            if (file.exists() && !file.delete()) {
                Log.e("FileManager", "Failed to delete: " + file.getName());
            }
        } catch (Exception e) {
            Log.e("FileManager", "Error deleting file: " + file.getUri(), e);
        }
    }
}

