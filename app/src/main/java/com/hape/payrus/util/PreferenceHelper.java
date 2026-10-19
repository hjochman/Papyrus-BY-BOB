package com.hape.payrus.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import androidx.preference.PreferenceManager;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;

public class PreferenceHelper {
    public static final String KEY_NOTES_DIR = "notesCustomPath";
    public static final String KEY_NOTES_URI = "notesCustomUri";
    public static final String KEY_SORT_ORDER = "sort_order";
    public static final String KEY_DIR_ORDER = "dir_order";
    public static final String KEY_COLORS = "colors";
    public static final String KEY_MONOSPACE = "monospaceFont";
    public static final String KEY_TEXT_SIZE = "text_size";
    public static final String KEY_CHANGE_NAME = "changeName";
    public static final String KEY_ELLIPSIS = "ellipsis";
    public static final String KEY_SEARCH_BAR = "searchBar";
    public static final String KEY_SEARCH_HISTORY = "searchHistory";
    public static final String KEY_LAST_SEEN_VERSION_CODE = "last_seen_version_code";
    public static final int MAX_SEARCH_HISTORY = 10;

    public static final String SORT_BY_DATE = "BY_DATE";
    public static final String SORT_BY_NAME = "BY_NAME";
    public static final String DIR_ON_TOP = "DIR_ON_TOP";
    public static final String DIR_ON_BOTTOM = "DIR_ON_BOTTOM";

    private final SharedPreferences prefs;
    private final Context context;

    public PreferenceHelper(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = PreferenceManager.getDefaultSharedPreferences(this.context);
    }

    public File getDefaultNotesDir() {
        File primary = Environment.getExternalStorageDirectory();
        File dir = new File(primary != null ? primary : context.getExternalFilesDir(null), "Notes");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public String getNotesDirPath() {
        String path = prefs.getString(KEY_NOTES_DIR, null);
        if (path == null || path.trim().isEmpty()) {
            return getDefaultNotesDir().getAbsolutePath();
        }
        return path;
    }

    public void setNotesDirPath(String path) {
        prefs.edit().putString(KEY_NOTES_DIR, path).apply();
    }

    public String getNotesTreeUri() {
        return prefs.getString(KEY_NOTES_URI, null);
    }

    public void setNotesTreeUri(String uriString) {
        prefs.edit().putString(KEY_NOTES_URI, uriString).apply();
    }

    public String getSortOrder() {
        return prefs.getString(KEY_SORT_ORDER, SORT_BY_DATE);
    }

    public void setSortOrder(String sortOrder) {
        prefs.edit().putString(KEY_SORT_ORDER, sortOrder).apply();
    }

    public String getDirOrder() {
        return prefs.getString(KEY_DIR_ORDER, DIR_ON_BOTTOM);
    }

    public void setDirOrder(String dirOrder) {
        prefs.edit().putString(KEY_DIR_ORDER, dirOrder).apply();
    }

    public boolean isMonospace() {
        return prefs.getBoolean(KEY_MONOSPACE, false);
    }

    public void setMonospace(boolean monospace) {
        prefs.edit().putBoolean(KEY_MONOSPACE, monospace).apply();
    }

    public String getTextSize() {
        return prefs.getString(KEY_TEXT_SIZE, "SIZE_MEDIUM");
    }

    public float getTextSizeSp() {
        String size = getTextSize();
        if ("SIZE_SMALL".equalsIgnoreCase(size)) return 14f;
        if ("SIZE_LARGE".equalsIgnoreCase(size)) return 20f;
        if ("SIZE_XLARGE".equalsIgnoreCase(size)) return 24f;
        return 16f; // MEDIUM
    }

    public boolean isChangeName() {
        return prefs.getBoolean(KEY_CHANGE_NAME, true);
    }

    public boolean isEllipsis() {
        return prefs.getBoolean(KEY_ELLIPSIS, true);
    }

    public boolean isSearchBarAlwaysVisible() {
        return prefs.getBoolean(KEY_SEARCH_BAR, false);
    }

    public String getColorsScheme() {
        return prefs.getString(KEY_COLORS, "COLORS_DEFAULT");
    }

    /** Returns the search history, newest entry first. */
    public List<String> getSearchHistory() {
        List<String> result = new ArrayList<>();
        String json = prefs.getString(KEY_SEARCH_HISTORY, null);
        if (json == null) return result;
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                result.add(array.getString(i));
            }
        } catch (JSONException e) {
            result.clear();
        }
        return result;
    }

    /** Adds a query to the history: trimmed, no duplicates, newest first, limited to {@link #MAX_SEARCH_HISTORY}. */
    public void addSearchHistory(String query) {
        if (query == null) return;
        String trimmed = query.trim();
        if (trimmed.isEmpty()) return;
        List<String> history = getSearchHistory();
        history.remove(trimmed);
        history.add(0, trimmed);
        while (history.size() > MAX_SEARCH_HISTORY) {
            history.remove(history.size() - 1);
        }
        prefs.edit().putString(KEY_SEARCH_HISTORY, new JSONArray(history).toString()).apply();
    }

    public void clearSearchHistory() {
        prefs.edit().remove(KEY_SEARCH_HISTORY).apply();
    }

    public long getLastSeenVersionCode() {
        return prefs.getLong(KEY_LAST_SEEN_VERSION_CODE, -1);
    }

    public void setLastSeenVersionCode(long versionCode) {
        prefs.edit().putLong(KEY_LAST_SEEN_VERSION_CODE, versionCode).apply();
    }
}
