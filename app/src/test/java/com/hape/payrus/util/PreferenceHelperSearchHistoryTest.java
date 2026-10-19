package com.hape.payrus.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class PreferenceHelperSearchHistoryTest {

    private PreferenceHelper prefs;

    @Before
    public void setUp() {
        prefs = new PreferenceHelper(RuntimeEnvironment.getApplication());
        prefs.clearSearchHistory();
    }

    @Test
    public void emptyByDefault() {
        assertTrue(prefs.getSearchHistory().isEmpty());
    }

    @Test
    public void newestFirst() {
        prefs.addSearchHistory("a");
        prefs.addSearchHistory("b");
        prefs.addSearchHistory("c");
        assertEquals(Arrays.asList("c", "b", "a"), prefs.getSearchHistory());
    }

    @Test
    public void duplicateMovesToFront() {
        prefs.addSearchHistory("a");
        prefs.addSearchHistory("b");
        prefs.addSearchHistory("a");
        assertEquals(Arrays.asList("a", "b"), prefs.getSearchHistory());
    }

    @Test
    public void trimsAndIgnoresBlank() {
        prefs.addSearchHistory("  foo  ");
        prefs.addSearchHistory("   ");
        prefs.addSearchHistory("");
        prefs.addSearchHistory(null);
        prefs.addSearchHistory("foo");
        assertEquals(Arrays.asList("foo"), prefs.getSearchHistory());
    }

    @Test
    public void limitedToTen_oldestDropped() {
        for (int i = 1; i <= 12; i++) {
            prefs.addSearchHistory("q" + i);
        }
        List<String> history = prefs.getSearchHistory();
        assertEquals(PreferenceHelper.MAX_SEARCH_HISTORY, history.size());
        assertEquals("q12", history.get(0));
        assertEquals("q3", history.get(9));
    }

    @Test
    public void specialCharactersSurviveRoundtrip() {
        prefs.addSearchHistory("a\"b,[c]\\ äöü Привет");
        assertEquals(Arrays.asList("a\"b,[c]\\ äöü Привет"), prefs.getSearchHistory());
    }

    @Test
    public void clearRemovesAll() {
        prefs.addSearchHistory("a");
        prefs.addSearchHistory("b");
        prefs.clearSearchHistory();
        assertTrue(prefs.getSearchHistory().isEmpty());
    }

    @Test
    public void persistedAcrossInstances() {
        prefs.addSearchHistory("x");
        PreferenceHelper other = new PreferenceHelper(RuntimeEnvironment.getApplication());
        assertEquals(Arrays.asList("x"), other.getSearchHistory());
    }
}
