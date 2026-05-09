package com.codeheadsystems.game.sample;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Preferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HighscoreStoreTest {

    private Preferences prefs;
    private HighscoreStore store;

    @BeforeEach
    void setUp() {
        prefs = mock(Preferences.class);
        when(prefs.getFloat(eq(HighscoreStore.KEY), eq(0f))).thenReturn(0f);
        // Stub putFloat to chain back; mock returns null by default and most callsites don't use it.
        when(prefs.putFloat(eq(HighscoreStore.KEY), org.mockito.ArgumentMatchers.anyFloat())).thenAnswer(inv -> {
            float v = inv.getArgument(1);
            // Subsequent reads should reflect the just-stored value.
            when(prefs.getFloat(eq(HighscoreStore.KEY), eq(0f))).thenReturn(v);
            return prefs;
        });
        store = new HighscoreStore(prefs);
    }

    @Test
    void getBestReturnsZeroWhenNoStoredValue() {
        assertEquals(0f, store.getBest(), 0f);
    }

    @Test
    void recordIfBestPersistsFirstScoreAndReturnsTrue() {
        boolean recorded = store.recordIfBest(12.5f);
        assertTrue(recorded);
        verify(prefs).putFloat(HighscoreStore.KEY, 12.5f);
        verify(prefs, atLeastOnce()).flush();
        assertEquals(12.5f, store.getBest(), 1e-5f);
    }

    @Test
    void recordIfBestRejectsLowerScores() {
        store.recordIfBest(20f);
        boolean recorded = store.recordIfBest(15f);
        assertFalse(recorded);
        // First write happened; second should not.
        verify(prefs, times(1)).putFloat(eq(HighscoreStore.KEY), org.mockito.ArgumentMatchers.anyFloat());
    }

    @Test
    void recordIfBestRejectsEqualScore() {
        store.recordIfBest(10f);
        boolean recorded = store.recordIfBest(10f);
        assertFalse(recorded, "tie does not beat existing record");
    }

    @Test
    void recordIfBestAcceptsStrictlyHigherScore() {
        store.recordIfBest(10f);
        boolean recorded = store.recordIfBest(11f);
        assertTrue(recorded);
        assertEquals(11f, store.getBest(), 1e-5f);
    }

    @Test
    void neverFlushesWhenNoNewRecord() {
        store.recordIfBest(20f);
        // Reset interaction count.
        org.mockito.Mockito.clearInvocations(prefs);
        store.recordIfBest(5f);
        verify(prefs, never()).putFloat(eq(HighscoreStore.KEY), org.mockito.ArgumentMatchers.anyFloat());
        verify(prefs, never()).flush();
    }
}
