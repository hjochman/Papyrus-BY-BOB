package com.hape.payrus.ui;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.hape.payrus.R;
import com.hape.payrus.data.CryptoManager;
import com.hape.payrus.data.NoteItem;
import com.hape.payrus.data.StorageManager;
import com.hape.payrus.ui.adapter.NoteAdapter;
import com.hape.payrus.util.PreferenceHelper;
import com.hape.payrus.util.StorageAccess;
import java.io.File;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_EDITOR = 1001;
    private static final int REQ_STORAGE_PERMISSION = 1002;

    private StorageManager storageManager;
    private PreferenceHelper preferenceHelper;
    private NoteAdapter adapter;

    private View layoutSearch;
    private EditText editSearch;
    private TextView textCurrentPath;
    private TextView textEmpty;
    private RecyclerView recyclerView;
    private String lastRootPath;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferenceHelper = new PreferenceHelper(this);
        storageManager = new StorageManager(this);

        initViews();
        handleIntent(getIntent());
        ensureStorageAccess();
        checkFirstRunOrUpdate();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (storageManager.navigateUp()) {
                    loadNotes();
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
        updateSearchVisibility();
        storageManager.getRootNotesDirectory();
        String rootPath = preferenceHelper.getNotesDirPath();
        if (lastRootPath != null && !lastRootPath.equals(rootPath)) {
            // Notizverzeichnis wurde in den Einstellungen geändert: sofort dorthin wechseln
            storageManager.setCurrentDirectory(storageManager.getRootNotesDirectory());
            editSearch.setText("");
        }
        lastRootPath = rootPath;
        loadNotes();
    }

    private void ensureStorageAccess() {
        if (StorageAccess.hasFullAccess(this)) {
            storageManager.getRootNotesDirectory();
            return;
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.storage_access_title)
                    .setMessage(R.string.storage_access_message)
                    .setPositiveButton(R.string.storage_access_grant, (d, w) -> {
                        try {
                            startActivity(StorageAccess.buildAllFilesAccessIntent(this));
                        } catch (Exception ignored) {
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        } else {
            androidx.core.app.ActivityCompat.requestPermissions(
                    this,
                    new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQ_STORAGE_PERMISSION
            );
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (Intent.ACTION_SEND.equals(action) && "text/plain".equals(intent.getType())) {
            String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (sharedText != null) {
                openEditor(null, sharedText);
            }
        }
    }

    private void initViews() {
        layoutSearch = findViewById(R.id.layoutSearch);
        editSearch = findViewById(R.id.editSearch);
        textCurrentPath = findViewById(R.id.textCurrentPath);
        textEmpty = findViewById(R.id.textEmpty);
        recyclerView = findViewById(R.id.recyclerView);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        DividerItemDecoration divider = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        android.graphics.drawable.Drawable dividerDrawable = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.divider_line);
        if (dividerDrawable != null) {
            divider.setDrawable(dividerDrawable);
        }
        recyclerView.addItemDecoration(divider);

        View buttonInfo = findViewById(R.id.buttonInfo);
        if (buttonInfo != null) {
            buttonInfo.setOnClickListener(v ->
                Toast.makeText(this, R.string.hint_long_press_menu, Toast.LENGTH_LONG).show()
            );
        }

        adapter = new NoteAdapter(this, new NoteAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(NoteItem item) {
                if (item.isDirectory()) {
                    storageManager.setCurrentDirectory(item.toFile());
                    loadNotes();
                } else {
                    openEditor(item.getPath(), null);
                }
            }

            @Override
            public void onItemLongClick(NoteItem item, View anchorView) {
                showItemMenu(item, anchorView);
            }
        });
        recyclerView.setAdapter(adapter);

        editSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                doSearch();
                return true;
            }
            return false;
        });

        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString();
                if (query.trim().isEmpty()) {
                    loadNotes();
                } else {
                    List<NoteItem> results = storageManager.searchNotes(query);
                    displayNotes(results);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void updateSearchVisibility() {
        if (preferenceHelper.isSearchBarAlwaysVisible()) {
            layoutSearch.setVisibility(View.VISIBLE);
        }
    }

    private void loadNotes() {
        textCurrentPath.setText(storageManager.getCurrentDirectory().getAbsolutePath());
        List<NoteItem> items = storageManager.listNotes();
        displayNotes(items);
    }

    private void displayNotes(List<NoteItem> items) {
        adapter.setItems(items);
        int noteCount = 0;
        if (items != null) {
            for (NoteItem item : items) {
                if (!item.isDirectory()) {
                    noteCount++;
                }
            }
        }
        setTitle(getString(R.string.app_name) + " (" + noteCount + ")");

        if (items == null || items.isEmpty()) {
            textEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            textEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void checkFirstRunOrUpdate() {
        long currentVersionCode = 0;
        try {
            android.content.pm.PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                currentVersionCode = pInfo.getLongVersionCode();
            } else {
                currentVersionCode = pInfo.versionCode;
            }
        } catch (Exception ignored) {
        }

        long lastSeenVersion = preferenceHelper.getLastSeenVersionCode();
        if (lastSeenVersion < currentVersionCode) {
            preferenceHelper.setLastSeenVersionCode(currentVersionCode);
            showAboutDialog();
        }
    }

    private void showAboutDialog() {
        String versionName = "";
        try {
            versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (android.content.pm.PackageManager.NameNotFoundException ignored) {
            // Version bleibt leer
        }
        String message = getString(R.string.about_version, versionName)
                + "\n\n" + getString(R.string.about_description)
                + "\n\n" + getString(R.string.about_github)
                + "\n\n" + getString(R.string.about_original)
                + "\n\n" + getString(R.string.about_hints_title) + "\n" + getString(R.string.about_hints);

        TextView text = new TextView(this);
        text.setText(message);
        text.setTextSize(14);
        text.setAutoLinkMask(Linkify.WEB_URLS);
        text.setMovementMethod(LinkMovementMethod.getInstance());
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        text.setPadding(pad, pad / 2, pad, 0);
        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        scroll.addView(text);

        new AlertDialog.Builder(this)
                .setTitle(R.string.about_title)
                .setIcon(R.mipmap.ic_launcher)
                .setView(scroll)
                .setPositiveButton(R.string.dlg_ok, null)
                .show();
    }


    private void showSearchHistoryDialog() {
        List<String> history = preferenceHelper.getSearchHistory();
        if (history.isEmpty()) {
            Toast.makeText(this, R.string.search_history_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.search_history_title);
        builder.setItems(history.toArray(new String[0]), (dialog, which) -> {
            String term = history.get(which);
            layoutSearch.setVisibility(View.VISIBLE);
            editSearch.setText(term);
            editSearch.setSelection(term.length());
        });
        builder.setNegativeButton(R.string.search_history_clear, (dialog, which) -> {
            preferenceHelper.clearSearchHistory();
            Toast.makeText(this, R.string.search_history_cleared, Toast.LENGTH_SHORT).show();
        });
        builder.setNeutralButton(R.string.dlg_cancel, null);
        builder.show();
    }

    private void doSearch() {
        String query = editSearch.getText().toString();
        if (!query.trim().isEmpty()) {
            preferenceHelper.addSearchHistory(query);
            List<NoteItem> results = storageManager.searchNotes(query);
            displayNotes(results);
            if (results.isEmpty()) {
                Toast.makeText(this, R.string.note_search_failed, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openEditor(@Nullable String filePath, @Nullable String initialText) {
        Intent intent = new Intent(this, EditorActivity.class);
        if (filePath != null) {
            intent.putExtra(EditorActivity.EXTRA_FILE_PATH, filePath);
        }
        if (initialText != null) {
            intent.putExtra(EditorActivity.EXTRA_INITIAL_TEXT, initialText);
        }
        intent.putExtra(EditorActivity.EXTRA_CURRENT_DIR, storageManager.getCurrentDirectory().getAbsolutePath());
        startActivityForResult(intent, REQUEST_EDITOR);
    }

    private void showItemMenu(NoteItem item, View anchor) {
        PopupMenu popup = new PopupMenu(
                new androidx.appcompat.view.ContextThemeWrapper(this, R.style.ThemeOverlay_Papyrus_Popup), anchor);
        android.text.SpannableString header = new android.text.SpannableString(item.getName());
        header.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                0, header.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        popup.getMenu().add(0, R.id.menu_item_header, 0, header);
        popup.getMenuInflater().inflate(R.menu.note_item_menu, popup.getMenu());

        if (item.isDirectory()) {
            popup.getMenu().findItem(R.id.action_send).setVisible(false);
            popup.getMenu().findItem(R.id.action_copy_to).setVisible(false);
            popup.getMenu().findItem(R.id.action_encrypt).setVisible(false);
            popup.getMenu().findItem(R.id.action_decrypt).setVisible(false);
        } else {
            boolean encrypted = isNoteEncrypted(item);
            popup.getMenu().findItem(R.id.action_encrypt).setVisible(!encrypted);
            popup.getMenu().findItem(R.id.action_decrypt).setVisible(encrypted);
        }

        popup.setOnMenuItemClickListener(menuItem -> {
            int id = menuItem.getItemId();
            if (id == R.id.menu_item_header) {
                return true;
            } else if (id == R.id.action_rename) {
                showRenameDialog(item);
                return true;
            } else if (id == R.id.action_encrypt) {
                showEncryptDialog(item);
                return true;
            } else if (id == R.id.action_decrypt) {
                showDecryptDialog(item);
                return true;
            } else if (id == R.id.action_delete) {
                showDeleteConfirmation(item);
                return true;
            } else if (id == R.id.action_send) {
                shareNote(item);
                return true;
            } else if (id == R.id.action_copy_to || id == R.id.action_move_to) {
                showMoveOrCopyDialog(item, id == R.id.action_copy_to);
                return true;
            }
            return false;
        });
        popup.show();
    }

    private boolean isNoteEncrypted(NoteItem item) {
        try {
            String content = storageManager.readNote(item.toFile());
            int newlineIdx = content.indexOf('\n');
            String body = (newlineIdx >= 0) ? content.substring(newlineIdx + 1) : content;
            return CryptoManager.isEncrypted(body);
        } catch (Exception e) {
            return false;
        }
    }

    private void showEncryptDialog(NoteItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.menu_encrypt);

        final EditText input = new EditText(this);
        input.setHint(R.string.dlg_password);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton(R.string.dlg_ok, (dialog, which) -> {
            String password = input.getText().toString();
            if (password.isEmpty()) {
                Toast.makeText(this, R.string.pe_no_password_entered, Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                String content = storageManager.readNote(item.toFile());
                int newlineIdx = content.indexOf('\n');
                String header = (newlineIdx >= 0) ? content.substring(0, newlineIdx) : content;
                String body = (newlineIdx >= 0) ? content.substring(newlineIdx + 1) : "";

                if (body.trim().isEmpty()) {
                    Toast.makeText(this, R.string.pe_empty_body, Toast.LENGTH_SHORT).show();
                    return;
                }

                String encryptedBody = CryptoManager.encrypt(body, password);
                storageManager.saveNote(item.getPath(), header + "\n" + encryptedBody, null);
                loadNotes();
                Toast.makeText(this, R.string.toast_note_saved, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, R.string.pe_failed_encrypt, Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(R.string.dlg_cancel, null);
        builder.show();
    }

    private void showDecryptDialog(NoteItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.menu_decrypt);

        final EditText input = new EditText(this);
        input.setHint(R.string.dlg_password);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton(R.string.dlg_ok, (dialog, which) -> {
            String password = input.getText().toString();
            if (password.isEmpty()) {
                Toast.makeText(this, R.string.pe_no_password_entered, Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                String content = storageManager.readNote(item.toFile());
                int newlineIdx = content.indexOf('\n');
                String header = (newlineIdx >= 0) ? content.substring(0, newlineIdx) : "";
                String body = (newlineIdx >= 0) ? content.substring(newlineIdx + 1) : content;

                if (!CryptoManager.isEncrypted(body)) {
                    Toast.makeText(this, R.string.pe_failed_decrypt, Toast.LENGTH_SHORT).show();
                    return;
                }

                String decryptedBody = CryptoManager.decrypt(body, password);
                String newContent = header.isEmpty() ? decryptedBody : header + "\n" + decryptedBody;
                storageManager.saveNote(item.getPath(), newContent, null);
                loadNotes();
                Toast.makeText(this, R.string.toast_password_match, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, R.string.pe_failed_decrypt, Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(R.string.dlg_cancel, null);
        builder.show();
    }

    private void showRenameDialog(NoteItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.menu_rename);

        final EditText input = new EditText(this);
        input.setText(item.getName());
        input.setSelection(item.getName().length());
        builder.setView(input);

        builder.setPositiveButton(R.string.dlg_ok, (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                boolean success = storageManager.rename(item.toFile(), newName);
                if (success) {
                    loadNotes();
                } else {
                    Toast.makeText(this, R.string.pe_failed, Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton(R.string.dlg_cancel, (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showDeleteConfirmation(NoteItem item) {
        new AlertDialog.Builder(this)
            .setMessage(R.string.dlg_delete_confirmation)
            .setPositiveButton(R.string.dlg_yes, (dialog, which) -> {
                boolean success = storageManager.delete(item.toFile());
                if (success) {
                    loadNotes();
                } else {
                    Toast.makeText(this, R.string.pe_failed, Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton(R.string.dlg_no, null)
            .show();
    }

    private void shareNote(NoteItem item) {
        try {
            String content = storageManager.readNote(item.toFile());
            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.setType("text/plain");
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, item.getName());
            sendIntent.putExtra(Intent.EXTRA_TEXT, content);
            startActivity(Intent.createChooser(sendIntent, getString(R.string.send_chooser)));
        } catch (Exception e) {
            Toast.makeText(this, R.string.pe_failed_load, Toast.LENGTH_SHORT).show();
        }
    }

    private void showMoveOrCopyDialog(NoteItem item, boolean isCopy) {
        // Show available folders to move/copy to
        File root = storageManager.getRootNotesDirectory();
        File[] subdirs = root.listFiles(File::isDirectory);

        if (subdirs == null || subdirs.length == 0) {
            Toast.makeText(this, R.string.pe_no_parent_dir, Toast.LENGTH_SHORT).show();
            return;
        }

        String[] names = new String[subdirs.length + 1];
        names[0] = root.getName() + " (" + getString(R.string.dlg_here) + ")";
        for (int i = 0; i < subdirs.length; i++) {
            names[i + 1] = subdirs[i].getName();
        }

        new AlertDialog.Builder(this)
            .setTitle(isCopy ? R.string.menu_copy_to : R.string.menu_move_to)
            .setItems(names, (dialog, which) -> {
                File targetDir = (which == 0) ? root : subdirs[which - 1];
                try {
                    boolean success;
                    if (isCopy) {
                        success = storageManager.copy(item.toFile(), targetDir);
                    } else {
                        success = storageManager.move(item.toFile(), targetDir);
                    }
                    if (success) {
                        loadNotes();
                    } else {
                        Toast.makeText(this, R.string.pe_failed, Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(this, R.string.pe_failed, Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton(R.string.dlg_cancel, null)
            .show();
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
                boolean success = storageManager.createDirectory(dirName);
                if (success) {
                    loadNotes();
                } else {
                    Toast.makeText(this, R.string.pe_failed, Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton(R.string.dlg_cancel, null);
        builder.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_new_note) {
            openEditor(null, null);
            return true;
        } else if (id == R.id.action_new_dir) {
            showNewDirDialog();
            return true;
        } else if (id == R.id.action_search) {
            layoutSearch.setVisibility(layoutSearch.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            return true;
        } else if (id == R.id.action_search_history) {
            showSearchHistoryDialog();
            return true;
        } else if (id == R.id.action_preferences) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.action_about) {
            showAboutDialog();
            return true;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_EDITOR) {
            loadNotes();
        }
    }
}
