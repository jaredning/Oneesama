package catgirl.oneesama.data.controller;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import org.apache.commons.io.FileUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import catgirl.oneesama.application.Application;
import catgirl.oneesama.data.model.chapter.ui.UiPage;
import catgirl.oneesama.data.settings.StorageSettings;

public class FileManager {

    public static String DOWNLOAD_FOLDER = "chapters";

    private static Map<Integer, Map<String, DocumentFile>> chapterFileCache = new HashMap<>();
    private static Map<Integer, DocumentFile> chapterDirCache = new HashMap<>();
    private static DocumentFile downloadDirCache = null;

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

        Map<String, DocumentFile> fileCache = new HashMap<>();

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

    public static DocumentFile getDownloadDirectory(Context context, boolean create) {
        if (downloadDirCache != null && downloadDirCache.exists()) {
            return downloadDirCache;
        }

        DocumentFile root = getRootFolder(context);
        if (root == null) return null;

        DocumentFile downloadDir = root.findFile(DOWNLOAD_FOLDER);

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
        }

        if (downloadDir != null) {
            downloadDirCache = downloadDir;
        }
        return downloadDir;
    }

    public static DocumentFile getChapterDirectory(Context context, int chapterId) {
        return getChapterDirectory(context, chapterId, true);
    }

    public static DocumentFile getChapterDirectory(Context context, int chapterId, boolean create) {
        if (chapterDirCache.containsKey(chapterId)) {
            DocumentFile cached = chapterDirCache.get(chapterId);
            if (cached != null && cached.exists()) {
                return cached;
            }
        }

        DocumentFile downloadDir = getDownloadDirectory(context, create);
        if (downloadDir == null) return null;

        String idStr = String.valueOf(chapterId);
        DocumentFile chapterDir = downloadDir.findFile(idStr);

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
        }

        if (chapterDir != null) {
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

    public static DocumentFile getPageDocumentFile(Context context, int chapterId, UiPage page) {
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
                fileCache = new HashMap<>();
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

    public static void deleteFolder(int chapterId) {
        clearCache(chapterId);
        DocumentFile chapterDir = getChapterDirectory(Application.getContextOfApplication(), chapterId, false);
        if (chapterDir != null) {
            chapterDir.delete();
            return;
        }

        // Some devices have bugs with re-creating formerly deleted directories, hence this workaround
        try {
            File folder = getChapterFolder(chapterId, false);
            if (folder != null && folder.exists()) {
                File deleted = new File(Application.getContextOfApplication().getExternalFilesDir(null), "deleted/" + chapterId);
                deleted.mkdirs();
                folder.renameTo(deleted);
                FileUtils.deleteDirectory(deleted);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
