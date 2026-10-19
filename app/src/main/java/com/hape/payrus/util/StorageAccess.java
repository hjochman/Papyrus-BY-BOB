package com.hape.payrus.util;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import androidx.core.content.ContextCompat;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Hilfsfunktionen für den Zugriff auf den gesamten Gerätespeicher (Root, SD-Karte). */
public final class StorageAccess {
    private StorageAccess() {}

    /** Liefert true, wenn die App beliebige Ordner im gemeinsamen Speicher lesen und schreiben darf. */
    public static boolean hasFullAccess(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    /** Intent zur Systemeinstellung "Zugriff auf alle Dateien" (nur Android 11+). */
    public static Intent buildAllFilesAccessIntent(Context context) {
        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        return intent;
    }

    /** Wurzelverzeichnisse aller Speichermedien (interner Speicher, SD-Karte, USB). */
    public static List<File> getStorageRoots(Context context) {
        List<File> roots = new ArrayList<>();
        File primary = Environment.getExternalStorageDirectory();
        if (primary != null) {
            roots.add(primary);
        }
        File[] appDirs = context.getExternalFilesDirs(null);
        if (appDirs != null) {
            for (File dir : appDirs) {
                if (dir == null) continue;
                String path = dir.getAbsolutePath();
                int idx = path.indexOf("/Android/data/");
                if (idx > 0) {
                    File root = new File(path.substring(0, idx));
                    if (!roots.contains(root)) {
                        roots.add(root);
                    }
                }
            }
        }
        return roots;
    }
}
