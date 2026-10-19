package com.hape.payrus.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import com.hape.payrus.util.PreferenceHelper;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class StorageManagerTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private PreferenceHelper prefs;
    private StorageManager storage;
    private File root;

    @Before
    public void setUp() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        root = tmp.newFolder("notes");
        prefs = new PreferenceHelper(context);
        prefs.setNotesDirPath(root.getAbsolutePath());
        storage = new StorageManager(context);
    }

    private static List<String> names(List<NoteItem> items) {
        List<String> result = new ArrayList<>();
        for (NoteItem item : items) {
            result.add(item.getName());
        }
        return result;
    }

    @Test
    public void saveAndReadNote_roundtrip() throws Exception {
        File saved = storage.saveNote(null, "Line1\nLine2 \u00e4\u00f6\u00fc", "My Note");

        assertEquals(new File(root, "My Note.txt"), saved);
        assertTrue(saved.exists());
        assertEquals("Line1\nLine2 \u00e4\u00f6\u00fc", storage.readNote(saved));
    }

    @Test
    public void saveNote_existingPath_overwrites() throws Exception {
        File saved = storage.saveNote(null, "old", "note");
        File again = storage.saveNote(saved.getAbsolutePath(), "new", "ignored");

        assertEquals(saved, again);
        assertEquals("new", storage.readNote(saved));
        assertEquals(1, storage.listNotes().size());
    }

    @Test
    public void saveNote_blankName_generatesFallbackName() throws Exception {
        File saved = storage.saveNote(null, "x", "   ");

        assertTrue(saved.getName().startsWith("Note_"));
        assertTrue(saved.getName().endsWith(".txt"));
    }

    @Test
    public void sanitizeFileName_replacesIllegalCharacters() {
        assertEquals("a_b_c", StorageManager.sanitizeFileName("a/b:c"));
        assertEquals("", StorageManager.sanitizeFileName(null));
        assertEquals("name", StorageManager.sanitizeFileName("  name  "));
    }

    @Test
    public void listNotes_stripsExtensionAndSetsPreview() throws Exception {
        storage.saveNote(null, "\n  \nFirst real line\nsecond", "alpha");

        List<NoteItem> items = storage.listNotes();

        assertEquals(1, items.size());
        assertEquals("alpha", items.get(0).getName());
        assertEquals("First real line", items.get(0).getPreview());
        assertFalse(items.get(0).isDirectory());
    }

    @Test
    public void createRenameDelete() throws Exception {
        assertTrue(storage.createDirectory("folder"));
        assertTrue(new File(root, "folder").isDirectory());
        assertFalse(storage.createDirectory("  "));

        File note = storage.saveNote(null, "content", "old");
        assertTrue(storage.rename(note, "renamed"));
        assertTrue(new File(root, "renamed.txt").exists());
        assertFalse(note.exists());

        File folder = new File(root, "folder");
        storage.saveNote(new File(folder, "inner.txt").getAbsolutePath(), "x", null);
        assertTrue(storage.delete(folder));
        assertFalse(folder.exists());

        assertTrue(storage.delete(new File(root, "renamed.txt")));
        assertTrue(storage.listNotes().isEmpty());
    }

    @Test
    public void copyAndMove() throws Exception {
        File note = storage.saveNote(null, "data", "n");
        assertTrue(storage.createDirectory("target"));
        File target = new File(root, "target");

        assertTrue(storage.copy(note, target));
        assertEquals("data", storage.readNote(new File(target, "n.txt")));
        assertTrue(note.exists());

        File other = storage.saveNote(null, "moved", "m");
        assertTrue(storage.move(other, target));
        assertFalse(other.exists());
        assertEquals("moved", storage.readNote(new File(target, "m.txt")));
        assertFalse(storage.copy(target, root));
    }

    @Test
    public void listNotes_sortByName_dirsOnBottomByDefault() throws Exception {
        prefs.setSortOrder(PreferenceHelper.SORT_BY_NAME);
        prefs.setDirOrder(PreferenceHelper.DIR_ON_BOTTOM);
        storage.saveNote(null, "x", "banana");
        storage.saveNote(null, "x", "Apple");
        storage.createDirectory("aaa-dir");

        assertEquals(List.of("Apple", "banana", "aaa-dir"), names(storage.listNotes()));
    }

    @Test
    public void listNotes_sortByName_dirsOnTop() throws Exception {
        prefs.setSortOrder(PreferenceHelper.SORT_BY_NAME);
        prefs.setDirOrder(PreferenceHelper.DIR_ON_TOP);
        storage.saveNote(null, "x", "banana");
        storage.saveNote(null, "x", "Apple");
        storage.createDirectory("zzz-dir");

        assertEquals(List.of("zzz-dir", "Apple", "banana"), names(storage.listNotes()));
    }

    @Test
    public void listNotes_sortByDate_newestFirst() throws Exception {
        prefs.setSortOrder(PreferenceHelper.SORT_BY_DATE);
        File older = storage.saveNote(null, "x", "older");
        File newer = storage.saveNote(null, "x", "newer");
        assertTrue(older.setLastModified(1_000_000_000_000L));
        assertTrue(newer.setLastModified(1_100_000_000_000L));

        assertEquals(List.of("newer", "older"), names(storage.listNotes()));
    }

    @Test
    public void navigation_intoSubdirAndUp() throws Exception {
        assertTrue(storage.createDirectory("sub"));
        File sub = new File(root, "sub");

        assertTrue(storage.isAtRoot());
        assertFalse(storage.navigateUp());

        storage.setCurrentDirectory(sub);
        assertFalse(storage.isAtRoot());
        storage.saveNote(null, "inside", "inner");
        assertTrue(new File(sub, "inner.txt").exists());

        assertTrue(storage.navigateUp());
        assertTrue(storage.isAtRoot());
    }

    @Test
    public void searchNotes_findsByNameContentAndFolder_caseInsensitive() throws Exception {
        storage.saveNote(null, "nothing", "Shopping List");
        storage.saveNote(null, "remember the MILK", "todo");
        storage.saveNote(null, "unrelated", "other");
        assertTrue(storage.createDirectory("Milkyway"));
        File sub = new File(root, "Milkyway");
        storage.saveNote(new File(sub, "deep.txt").getAbsolutePath(), "has milk too", null);

        List<String> byContentOrName = names(storage.searchNotes("milk"));
        assertEquals(3, byContentOrName.size());
        assertTrue(byContentOrName.contains("todo"));
        assertTrue(byContentOrName.contains("deep"));
        assertTrue(byContentOrName.contains("Milkyway"));

        assertEquals(List.of("Shopping List"), names(storage.searchNotes("  SHOPPING ")));
        assertTrue(storage.searchNotes("zzzz-no-match").isEmpty());
    }

    @Test
    public void searchNotes_blankQuery_returnsCurrentListing() throws Exception {
        storage.saveNote(null, "x", "one");
        storage.saveNote(null, "x", "two");

        assertEquals(2, storage.searchNotes("  ").size());
        assertEquals(2, storage.searchNotes(null).size());
    }
}
