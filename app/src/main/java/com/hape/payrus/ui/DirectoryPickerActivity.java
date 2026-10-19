package com.hape.payrus.ui;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.hape.payrus.R;
import com.hape.payrus.util.StorageAccess;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DirectoryPickerActivity extends AppCompatActivity {
    public static final String EXTRA_INITIAL_PATH = "extra_initial_path";
    public static final String RESULT_SELECTED_PATH = "result_selected_path";

    private File currentDir;
    private TextView textCurrentDirectory;
    private RecyclerView recyclerView;
    private DirAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_directory_picker);
        setTitle(R.string.pref_dir);

        String initialPath = getIntent().getStringExtra(EXTRA_INITIAL_PATH);
        if (initialPath != null && new File(initialPath).exists()) {
            currentDir = new File(initialPath);
        } else {
            currentDir = Environment.getExternalStorageDirectory();
            if (currentDir == null || !currentDir.exists()) {
                currentDir = getFilesDir();
            }
        }

        initViews();
        loadDirectories();
        ensureStorageAccess();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (canNavigateUp()) {
                    currentDir = currentDir.getParentFile();
                    loadDirectories();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null && currentDir != null) {
            loadDirectories();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        loadDirectories();
    }

    /** Ohne Vollzugriff sind nur App-eigene Ordner wählbar. Nutzer zur Freigabe führen. */
    private void ensureStorageAccess() {
        if (StorageAccess.hasFullAccess(this)) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.storage_access_title)
                    .setMessage(R.string.storage_access_message)
                    .setPositiveButton(R.string.storage_access_grant, (d, w) -> {
                        try {
                            startActivity(StorageAccess.buildAllFilesAccessIntent(this));
                        } catch (ActivityNotFoundException e) {
                            startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
                        }
                    })
                    .setNegativeButton(R.string.dlg_cancel, null)
                    .show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2001);
        }
    }

    private void initViews() {
        textCurrentDirectory = findViewById(R.id.textCurrentDirectory);
        recyclerView = findViewById(R.id.recyclerViewDirs);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        adapter = new DirAdapter(file -> {
            currentDir = file;
            loadDirectories();
        });
        recyclerView.setAdapter(adapter);
        textCurrentDirectory.setOnClickListener(v -> showStorageRootsDialog());

        Button buttonChooseHere = findViewById(R.id.buttonChooseHere);
        buttonChooseHere.setOnClickListener(v -> {
            Intent data = new Intent();
            data.putExtra(RESULT_SELECTED_PATH, currentDir.getAbsolutePath());
            setResult(Activity.RESULT_OK, data);
            finish();
        });

        Button buttonNewDir = findViewById(R.id.buttonNewDir);
        buttonNewDir.setOnClickListener(v -> showNewDirDialog());

        findViewById(R.id.buttonPhoneRoot).setOnClickListener(v -> {
            File phoneRoot = Environment.getExternalStorageDirectory();
            if (phoneRoot != null && phoneRoot.exists()) {
                currentDir = phoneRoot;
                loadDirectories();
            }
        });

        findViewById(R.id.buttonSdCardRoot).setOnClickListener(v -> {
            File sdRoot = findSdCardRoot();
            if (sdRoot != null) {
                currentDir = sdRoot;
                loadDirectories();
            } else {
                Toast.makeText(this, R.string.picker_no_sdcard, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** Erstes Speichermedium, das nicht der interne Speicher ist. */
    @Nullable
    private File findSdCardRoot() {
        File primary = Environment.getExternalStorageDirectory();
        for (File root : StorageAccess.getStorageRoots(this)) {
            if (!root.equals(primary) && root.exists()) {
                return root;
            }
        }
        return null;
    }

    private void showStorageRootsDialog() {
        List<File> roots = StorageAccess.getStorageRoots(this);
        String[] labels = new String[roots.size()];
        for (int i = 0; i < labels.length; i++) {
            labels[i] = roots.get(i).getAbsolutePath();
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.storage_roots_title)
                .setItems(labels, (d, which) -> {
                    currentDir = roots.get(which);
                    loadDirectories();
                })
                .setNegativeButton(R.string.dlg_cancel, null)
                .show();
    }

    private boolean canNavigateUp() {
        return currentDir != null && currentDir.getParentFile() != null && currentDir.getParentFile().canRead();
    }

    private void loadDirectories() {
        textCurrentDirectory.setText(currentDir.getAbsolutePath());
        List<File> dirList = new ArrayList<>();

        if (canNavigateUp()) {
            // Parent directory item representation
            dirList.add(new File(currentDir.getParent(), ".."));
        }

        File[] files = currentDir.listFiles();
        if (files != null) {
            List<File> subdirs = new ArrayList<>();
            for (File f : files) {
                if (f.isDirectory() && !f.isHidden() && f.canRead()) {
                    subdirs.add(f);
                }
            }
            Collections.sort(subdirs, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));
            dirList.addAll(subdirs);
        }

        adapter.setItems(dirList);
    }

    private void showNewDirDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.menu_new_dir);

        final EditText input = new EditText(this);
        input.setHint(R.string.dlg_dir_name);
        builder.setView(input);

        builder.setPositiveButton(R.string.dlg_ok, (dialog, which) -> {
            String dirName = input.getText().toString().trim();
            if (!dirName.isEmpty()) {
                File newFolder = new File(currentDir, dirName);
                if (newFolder.mkdirs()) {
                    currentDir = newFolder;
                    loadDirectories();
                } else {
                    Toast.makeText(this, R.string.pe_failed, Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton(R.string.dlg_cancel, null);
        builder.show();
    }

    static class DirAdapter extends RecyclerView.Adapter<DirAdapter.ViewHolder> {
        interface OnDirClickListener {
            void onDirClick(File dir);
        }

        private final List<File> items = new ArrayList<>();
        private final OnDirClickListener listener;

        DirAdapter(OnDirClickListener listener) {
            this.listener = listener;
        }

        void setItems(List<File> files) {
            items.clear();
            if (files != null) items.addAll(files);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_directory, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            File file = items.get(position);
            String name = file.getName();
            if ("..".equals(name)) {
                holder.textDirName.setText(".. (Parent Directory)");
            } else {
                holder.textDirName.setText(name);
            }

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    if ("..".equals(file.getName())) {
                        listener.onDirClick(file.getParentFile());
                    } else {
                        listener.onDirClick(file);
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView textDirName;
            ImageView imageDirIcon;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                textDirName = itemView.findViewById(R.id.textDirName);
                imageDirIcon = itemView.findViewById(R.id.imageDirIcon);
            }
        }
    }
}
