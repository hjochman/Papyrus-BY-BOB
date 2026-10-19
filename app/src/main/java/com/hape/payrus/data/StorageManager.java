package com.hape.payrus.data;

import android.content.Context;
import com.hape.payrus.util.PreferenceHelper;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StorageManager {
    private final Context context;
    private final PreferenceHelper preferenceHelper;
    private File currentDirectory;

    public StorageManager(Context context) {
        this.context = context.getApplicationContext();
        this.preferenceHelper = new PreferenceHelper(context);
        this.currentDirectory = getRootNotesDirectory();
    }

    public File getRootNotesDirectory() {
        String path = preferenceHelper.getNotesDirPath();
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public File getCurrentDirectory() {
        if (currentDirectory == null || !currentDirectory.exists()) {
            currentDirectory = getRootNotesDirectory();
        }
        return currentDirectory;
    }

    public void setCurrentDirectory(File dir) {
        if (dir != null && dir.exists() && dir.isDirectory()) {
            this.currentDirectory = dir;
        }
    }

    public boolean navigateUp() {
        File root = getRootNotesDirectory();
        if (currentDirectory.equals(root) || !currentDirectory.getAbsolutePath().startsWith(root.getAbsolutePath())) {
            return false;
        }
        File parent = currentDirectory.getParentFile();
        if (parent != null && parent.exists() && parent.getAbsolutePath().length() >= root.getAbsolutePath().length()) {
            currentDirectory = parent;
            return true;
        }
        return false;
    }

    public boolean isAtRoot() {
        return getCurrentDirectory().equals(getRootNotesDirectory());
    }

    public List<NoteItem> listNotes() {
        List<NoteItem> items = new ArrayList<>();
        File current = getCurrentDirectory();
        File[] files = current.listFiles();

        if (files == null) {
            return items;
        }

        for (File file : files) {
            if (file.isHidden()) continue;
            boolean isDir = file.isDirectory();
            String name = file.getName();
            
            // If it's a file, strip .txt extension for display if desired, or keep as is
            if (!isDir && name.endsWith(".txt")) {
                name = name.substring(0, name.length() - 4);
            }

            NoteItem item = new NoteItem(
                name,
                file.getAbsolutePath(),
                isDir,
                file.lastModified(),
                file.length()
            );

            if (!isDir) {
                item.setPreview(readFirstLine(file));
            }

            items.add(item);
        }

        sortItems(items);
        return items;
    }

    private void sortItems(List<NoteItem> items) {
        final String sortOrder = preferenceHelper.getSortOrder();
        final boolean dirOnTop = PreferenceHelper.DIR_ON_TOP.equals(preferenceHelper.getDirOrder());

        Collections.sort(items, (o1, o2) -> {
            if (o1.isDirectory() != o2.isDirectory()) {
                if (dirOnTop) {
                    return o1.isDirectory() ? -1 : 1;
                } else {
                    return o1.isDirectory() ? 1 : -1;
                }
            }

            if (PreferenceHelper.SORT_BY_NAME.equals(sortOrder)) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            } else {
                // By Date descending (newest first)
                return Long.compare(o2.getLastModified(), o1.getLastModified());
            }
        });
    }

    public String readFirstLine(File file) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    return line;
                }
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    public String readNote(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (!first) {
                    sb.append("\n");
                }
                sb.append(line);
                first = false;
            }
        }
        return sb.toString();
    }

    public File saveNote(String originalFilePath, String content, String proposedName) throws IOException {
        File fileToSave;
        if (originalFilePath != null && !originalFilePath.isEmpty()) {
            fileToSave = new File(originalFilePath);
        } else {
            String safeName = sanitizeFileName(proposedName);
            if (safeName.isEmpty()) {
                safeName = "Note_" + System.currentTimeMillis();
            }
            if (!safeName.endsWith(".txt")) {
                safeName += ".txt";
            }
            fileToSave = new File(getCurrentDirectory(), safeName);
        }

        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(fileToSave), StandardCharsets.UTF_8)) {
            writer.write(content);
            writer.flush();
        }

        return fileToSave;
    }

    public boolean createDirectory(String name) {
        String safeName = sanitizeFileName(name);
        if (safeName.isEmpty()) return false;
        File newDir = new File(getCurrentDirectory(), safeName);
        return newDir.mkdirs();
    }

    public boolean rename(File source, String newName) {
        String safeName = sanitizeFileName(newName);
        if (safeName.isEmpty()) return false;
        if (!source.isDirectory() && !safeName.endsWith(".txt")) {
            safeName += ".txt";
        }
        File dest = new File(source.getParentFile(), safeName);
        return source.renameTo(dest);
    }

    public boolean delete(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    delete(child);
                }
            }
        }
        return file.delete();
    }

    public boolean copy(File source, File targetDir) throws IOException {
        if (source.isDirectory()) return false;
        File dest = new File(targetDir, source.getName());
        try (FileInputStream in = new FileInputStream(source);
             FileOutputStream out = new FileOutputStream(dest)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
        }
        return true;
    }

    public boolean move(File source, File targetDir) {
        File dest = new File(targetDir, source.getName());
        return source.renameTo(dest);
    }

    public List<NoteItem> searchNotes(String query) {
        List<NoteItem> matches = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            return listNotes();
        }
        searchRecursive(getRootNotesDirectory(), query.toLowerCase().trim(), matches);
        sortItems(matches);
        return matches;
    }

    private void searchRecursive(File dir, String query, List<NoteItem> results) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isHidden()) continue;
            if (file.isDirectory()) {
                if (file.getName().toLowerCase().contains(query)) {
                    results.add(new NoteItem(file.getName(), file.getAbsolutePath(), true, file.lastModified(), file.length()));
                }
                searchRecursive(file, query, results);
            } else if (file.getName().endsWith(".txt")) {
                String name = file.getName();
                String displayName = name.substring(0, name.length() - 4);
                boolean match = displayName.toLowerCase().contains(query);
                String firstLine = readFirstLine(file);

                if (!match) {
                    try {
                        String content = readNote(file);
                        if (content.toLowerCase().contains(query)) {
                            match = true;
                        }
                    } catch (Exception ignored) {
                    }
                }

                if (match) {
                    NoteItem item = new NoteItem(displayName, file.getAbsolutePath(), false, file.lastModified(), file.length());
                    item.setPreview(firstLine);
                    results.add(item);
                }
            }
        }
    }

    public static String sanitizeFileName(String name) {
        if (name == null) return "";
        return name.replaceAll("[\\\\/:*?\"<>|\\r\\n]+", "_").trim();
    }
}
