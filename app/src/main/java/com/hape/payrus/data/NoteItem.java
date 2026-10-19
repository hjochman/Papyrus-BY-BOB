package com.hape.payrus.data;

import android.net.Uri;
import java.io.File;
import java.io.Serializable;

public class NoteItem implements Serializable, Comparable<NoteItem> {
    private String name;
    private String path;
    private String uriString;
    private boolean isDirectory;
    private long lastModified;
    private long size;
    private String preview;

    public NoteItem(String name, String path, boolean isDirectory, long lastModified, long size) {
        this.name = name;
        this.path = path;
        this.isDirectory = isDirectory;
        this.lastModified = lastModified;
        this.size = size;
    }

    public NoteItem(String name, String uriString, String path, boolean isDirectory, long lastModified, long size) {
        this.name = name;
        this.uriString = uriString;
        this.path = path;
        this.isDirectory = isDirectory;
        this.lastModified = lastModified;
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getUriString() {
        return uriString;
    }

    public Uri getUri() {
        return uriString != null ? Uri.parse(uriString) : null;
    }

    public void setUriString(String uriString) {
        this.uriString = uriString;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public void setDirectory(boolean directory) {
        isDirectory = directory;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getPreview() {
        return preview;
    }

    public void setPreview(String preview) {
        this.preview = preview;
    }

    public File toFile() {
        return path != null ? new File(path) : null;
    }

    @Override
    public int compareTo(NoteItem other) {
        if (other == null) return 1;
        if (this.isDirectory && !other.isDirectory) return -1;
        if (!this.isDirectory && other.isDirectory) return 1;
        return this.name.compareToIgnoreCase(other.name);
    }
}
