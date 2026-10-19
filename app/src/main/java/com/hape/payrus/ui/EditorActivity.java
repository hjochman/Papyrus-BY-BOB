package com.hape.payrus.ui;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.text.util.Linkify;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.hape.payrus.R;
import com.hape.payrus.data.CryptoManager;
import com.hape.payrus.data.StorageManager;
import com.hape.payrus.util.PreferenceHelper;
import java.io.File;
import java.util.Date;

public class EditorActivity extends AppCompatActivity {
    public static final String EXTRA_FILE_PATH = "extra_file_path";
    public static final String EXTRA_INITIAL_TEXT = "extra_initial_text";
    public static final String EXTRA_CURRENT_DIR = "extra_current_dir";

    private StorageManager storageManager;
    private PreferenceHelper preferenceHelper;

    private String originalFilePath;
    private String currentDirectoryPath;
    private EditText editor;
    private View layoutContextSearch;
    private EditText editContextSearch;
    private boolean isModified = false;
    private boolean linksShown = false;
    private int lastSearchIndex = -1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        storageManager = new StorageManager(this);
        preferenceHelper = new PreferenceHelper(this);

        initViews();
        loadFromIntent();
        applyPreferences();
    }

    private void initViews() {
        editor = findViewById(R.id.editor);
        layoutContextSearch = findViewById(R.id.layoutContextSearch);
        editContextSearch = findViewById(R.id.editContextSearch);

        ImageButton buttonContextForward = findViewById(R.id.buttonContextForward);
        ImageButton buttonContextReverse = findViewById(R.id.buttonContextReverse);

        buttonContextForward.setOnClickListener(v -> searchNext(true));
        buttonContextReverse.setOnClickListener(v -> searchNext(false));

        editor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                isModified = true;
                updateTitleFromContent();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        editContextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                lastSearchIndex = -1;
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadFromIntent() {
        Intent intent = getIntent();
        if (intent == null) return;

        originalFilePath = intent.getStringExtra(EXTRA_FILE_PATH);
        currentDirectoryPath = intent.getStringExtra(EXTRA_CURRENT_DIR);
        if (currentDirectoryPath != null) {
            storageManager.setCurrentDirectory(new File(currentDirectoryPath));
        }

        if (originalFilePath != null) {
            File file = new File(originalFilePath);
            if (file.exists()) {
                try {
                    String content = storageManager.readNote(file);
                    editor.setText(content);
                    editor.setSelection(editor.getText().length());
                    isModified = false;
                    updateTitleFromContent();
                } catch (Exception e) {
                    Toast.makeText(this, R.string.pe_failed_load, Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            String initialText = intent.getStringExtra(EXTRA_INITIAL_TEXT);
            if (initialText != null) {
                editor.setText(initialText);
                editor.setSelection(editor.getText().length());
                isModified = true;
                updateTitleFromContent();
            } else {
                setTitle(R.string.editor_new_note_title);
            }
        }
    }

    private void applyPreferences() {
        // Monospace font
        if (preferenceHelper.isMonospace()) {
            editor.setTypeface(Typeface.MONOSPACE);
        } else {
            editor.setTypeface(Typeface.DEFAULT);
        }

        // Text size
        editor.setTextSize(preferenceHelper.getTextSizeSp());

        // Color schemes
        String scheme = preferenceHelper.getColorsScheme();
        View root = findViewById(R.id.layoutEditorRoot);
        if ("COLORS_W_ON_B".equalsIgnoreCase(scheme)) {
            root.setBackgroundColor(Color.BLACK);
            editor.setTextColor(Color.WHITE);
            editor.setHintTextColor(Color.parseColor("#888888"));
        } else if ("COLORS_B_ON_W".equalsIgnoreCase(scheme)) {
            root.setBackgroundColor(Color.WHITE);
            editor.setTextColor(Color.BLACK);
            editor.setHintTextColor(Color.parseColor("#777777"));
        } else {
            root.setBackgroundResource(R.drawable.bg_papyrus);
            editor.setTextColor(Color.parseColor("#2B241E"));
            editor.setHintTextColor(Color.parseColor("#8C8070"));
        }
    }

    private void updateTitleFromContent() {
        String text = editor.getText().toString().trim();
        if (text.isEmpty()) {
            setTitle(R.string.editor_new_note_title);
            return;
        }

        String[] lines = text.split("\n");
        String firstLine = "";
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                firstLine = trimmed;
                break;
            }
        }

        if (firstLine.isEmpty()) {
            setTitle(R.string.editor_new_note_title);
            return;
        }

        if (firstLine.length() > 40) {
            firstLine = firstLine.substring(0, 40) + "...";
        } else if (lines.length > 1 && preferenceHelper.isEllipsis() && !firstLine.endsWith("...")) {
            firstLine = firstLine + "...";
        }
        setTitle(firstLine);
    }

    private void saveNote() {
        String content = editor.getText().toString();
        if (content.trim().isEmpty() && originalFilePath == null) {
            finish();
            return;
        }

        String proposedName = getTitle().toString().replace("...", "").trim();
        if (proposedName.isEmpty() || getString(R.string.editor_new_note_title).equals(proposedName)) {
            proposedName = "Note_" + System.currentTimeMillis();
        }

        try {
            File savedFile = storageManager.saveNote(originalFilePath, content, proposedName);
            originalFilePath = savedFile.getAbsolutePath();
            isModified = false;
            Toast.makeText(this, R.string.toast_note_saved, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, R.string.pe_failed_save, Toast.LENGTH_SHORT).show();
        }
    }

    private void searchNext(boolean forward) {
        String query = editContextSearch.getText().toString();
        if (query.isEmpty()) return;

        String fullText = editor.getText().toString().toLowerCase();
        query = query.toLowerCase();

        int targetIndex;
        if (forward) {
            int start = (lastSearchIndex >= 0) ? lastSearchIndex + 1 : editor.getSelectionStart();
            targetIndex = fullText.indexOf(query, start);
            if (targetIndex < 0) {
                // Loop back to beginning
                targetIndex = fullText.indexOf(query, 0);
            }
        } else {
            int start = (lastSearchIndex >= 0) ? lastSearchIndex - 1 : editor.getSelectionStart() - 1;
            if (start < 0) start = fullText.length();
            targetIndex = fullText.lastIndexOf(query, start);
        }

        if (targetIndex >= 0) {
            lastSearchIndex = targetIndex;
            editor.setSelection(targetIndex, targetIndex + query.length());
            editor.requestFocus();
        } else {
            Toast.makeText(this, R.string.note_search_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void insertDate() {
        java.text.DateFormat dateFormat = DateFormat.getDateFormat(this);
        java.text.DateFormat timeFormat = DateFormat.getTimeFormat(this);
        String formatted = dateFormat.format(new Date()) + " " + timeFormat.format(new Date());

        int start = Math.max(editor.getSelectionStart(), 0);
        int end = Math.max(editor.getSelectionEnd(), 0);
        editor.getText().replace(Math.min(start, end), Math.max(start, end), formatted, 0, formatted.length());
    }

    private void toggleLinks(MenuItem item) {
        linksShown = !linksShown;
        if (linksShown) {
            Linkify.addLinks(editor, Linkify.ALL);
            if (item != null) item.setTitle(R.string.menu_links_hide);
        } else {
            editor.setAutoLinkMask(0);
            editor.setText(editor.getText().toString());
            if (item != null) item.setTitle(R.string.menu_links_show);
        }
    }

    private void promptEncrypt() {
        String text = editor.getText().toString();
        int newlineIdx = text.indexOf('\n');
        String header = (newlineIdx >= 0) ? text.substring(0, newlineIdx) : text;
        String body = (newlineIdx >= 0) ? text.substring(newlineIdx + 1) : "";

        if (body.trim().isEmpty()) {
            Toast.makeText(this, R.string.pe_empty_body, Toast.LENGTH_SHORT).show();
            return;
        }

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
                String encryptedBody = CryptoManager.encrypt(body, password);
                editor.setText(header + "\n" + encryptedBody);
                Toast.makeText(this, R.string.toast_note_saved, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, R.string.pe_failed_encrypt, Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(R.string.dlg_cancel, null);
        builder.show();
    }

    private void promptDecrypt() {
        String text = editor.getText().toString();
        int newlineIdx = text.indexOf('\n');
        String header = (newlineIdx >= 0) ? text.substring(0, newlineIdx) : "";
        String body = (newlineIdx >= 0) ? text.substring(newlineIdx + 1) : text;

        if (!CryptoManager.isEncrypted(body)) {
            Toast.makeText(this, R.string.pe_failed_decrypt, Toast.LENGTH_SHORT).show();
            return;
        }

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
                String decryptedBody = CryptoManager.decrypt(body, password);
                editor.setText(header.isEmpty() ? decryptedBody : header + "\n" + decryptedBody);
                Toast.makeText(this, R.string.toast_password_match, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, R.string.pe_failed_decrypt, Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(R.string.dlg_cancel, null);
        builder.show();
    }

    private void sendNote() {
        String text = editor.getText().toString();
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, getTitle().toString());
        sendIntent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(sendIntent, getString(R.string.send_chooser)));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.editor_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_save) {
            saveNote();
            return true;
        } else if (id == R.id.action_search) {
            layoutContextSearch.setVisibility(layoutContextSearch.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            return true;
        } else if (id == R.id.action_insert_date) {
            insertDate();
            return true;
        } else if (id == R.id.action_encrypt) {
            promptEncrypt();
            return true;
        } else if (id == R.id.action_decrypt) {
            promptDecrypt();
            return true;
        } else if (id == R.id.action_links) {
            toggleLinks(item);
            return true;
        } else if (id == R.id.action_send) {
            sendNote();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isModified) {
            saveNote();
        }
    }
}
