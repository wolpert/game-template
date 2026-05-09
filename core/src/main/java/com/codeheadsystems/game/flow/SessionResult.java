package com.codeheadsystems.game.flow;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Generic carrier for end-of-session display data consumed by
 * {@link com.codeheadsystems.game.screens.GameOverScreen}. Game code populates the fields
 * before navigating to the game-over screen; the screen reads them on {@code show()}.
 *
 * <p>Lives in the scaffold (not under {@code session/}, which is demo-only) so the game-over
 * screen has no compile-time dependency on demo classes.
 */
@Singleton
public class SessionResult {

    /** Title text shown at the top of the game-over screen. */
    public String headline;
    /** Secondary line for stats / details (e.g. "Time: 12.3s"). May be empty. */
    public String detail;
    /** Whether the "Try Again" button should be active. */
    public boolean retryAvailable;
    /**
     * Action invoked when the player clicks "Try Again". Defaults to a no-op so the
     * scaffold's empty {@code GameScreen} reaching game-over is safe; the demo's
     * {@code SampleGameScreen} populates this with its own re-entry hop so
     * {@link com.codeheadsystems.game.screens.GameOverScreen} stays free of
     * sample-specific navigation knowledge.
     */
    public Runnable onRetry;

    @Inject
    public SessionResult() {
        reset();
    }

    /** Restore neutral defaults — useful before reusing the holder for another session. */
    public void reset() {
        headline = "Session Complete";
        detail = "";
        retryAvailable = true;
        onRetry = () -> {};
    }
}
