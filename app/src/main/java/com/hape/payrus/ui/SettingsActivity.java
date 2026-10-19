package com.hape.payrus.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import com.hape.payrus.R;
import com.hape.payrus.util.PreferenceHelper;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.activity_preferences);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        private Preference dirPreference;
        private PreferenceHelper preferenceHelper;

        private final ActivityResultLauncher<Intent> dirPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    String selectedPath = result.getData().getStringExtra(DirectoryPickerActivity.RESULT_SELECTED_PATH);
                    if (selectedPath != null && !selectedPath.isEmpty()) {
                        preferenceHelper.setNotesDirPath(selectedPath);
                        updateDirSummary(selectedPath);
                    }
                }
            });

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);

            preferenceHelper = new PreferenceHelper(requireContext());
            dirPreference = findPreference("notesCustomPath");

            if (dirPreference != null) {
                updateDirSummary(preferenceHelper.getNotesDirPath());
                dirPreference.setOnPreferenceClickListener(preference -> {
                    Intent intent = new Intent(requireContext(), DirectoryPickerActivity.class);
                    intent.putExtra(DirectoryPickerActivity.EXTRA_INITIAL_PATH, preferenceHelper.getNotesDirPath());
                    dirPickerLauncher.launch(intent);
                    return true;
                });
            }
        }

        private void updateDirSummary(String path) {
            if (dirPreference != null) {
                dirPreference.setSummary(path);
            }
        }
    }
}
