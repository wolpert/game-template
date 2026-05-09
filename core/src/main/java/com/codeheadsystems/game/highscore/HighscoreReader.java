package com.codeheadsystems.game.highscore;

/**
 * Scaffold-side read interface for a single best-score-per-mode value (in seconds-survived).
 * Lives in scaffold so {@code MainMenuScreen} can render a "Best: X.Xs" line without importing
 * anything from the sample package — the implementation is sample-side and bound through the
 * {@code @Sample}-qualified optional slot in {@code CoreModule}, mirroring the existing
 * {@code @Sample InputGate} / {@code @Sample Screen} pattern.
 */
public interface HighscoreReader {

    /**
     * Best score recorded so far, in seconds. Returns 0 when no score has been persisted.
     */
    float getBest();
}
