package com.codeheadsystems.game.sample;

import com.badlogic.gdx.Preferences;
import com.codeheadsystems.game.highscore.HighscoreReader;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Persists the dodge sample's best survival time using libGDX {@link Preferences}. Sample-only —
 * the scaffold accesses this through the {@link HighscoreReader} interface bound via the
 * {@code @Sample} optional slot in {@code CoreModule}, so deleting {@code SampleModule} drops
 * the binding and {@code MainMenuScreen}'s {@code Optional<HighscoreReader>} resolves to empty.
 */
@Singleton
public class HighscoreStore implements HighscoreReader {

    /** Key is namespaced by sample so future modes can persist their own bests independently. */
    static final String KEY = "highscore.sample.best.elapsedSec";

    private final Preferences prefs;

    @Inject
    public HighscoreStore(Preferences prefs) {
        this.prefs = prefs;
    }

    @Override
    public float getBest() {
        return prefs.getFloat(KEY, 0f);
    }

    /**
     * Records {@code elapsedSec} as the new best iff it strictly exceeds the stored value.
     * Flushes immediately so a crash on the next frame doesn't lose a fresh record.
     *
     * @return true if a new record was written
     */
    public boolean recordIfBest(float elapsedSec) {
        float current = getBest();
        if (elapsedSec > current) {
            prefs.putFloat(KEY, elapsedSec);
            prefs.flush();
            return true;
        }
        return false;
    }
}
