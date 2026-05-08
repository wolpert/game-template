package com.codeheadsystems.game.session;

import javax.inject.Inject;
import javax.inject.Singleton;

/** Mutable per-session state — reset by {@link com.codeheadsystems.game.screens.GameScreen} on entry. */
@Singleton
public class GameState {

    /**
     * Session lifecycle. PLAYING accepts input and spawns blocks; DYING is the in-between where
     * input/spawning are paused and the death animation runs to completion; GAME_OVER signals
     * GameScreen to transition to the game-over screen.
     */
    public enum Phase { PLAYING, DYING, GAME_OVER }

    public static final int MAX_HP = 5;

    public int hp;
    public float elapsedSec;
    public Phase phase;

    @Inject
    public GameState() {
        reset();
    }

    public void reset() {
        hp = MAX_HP;
        elapsedSec = 0f;
        phase = Phase.PLAYING;
    }

    public boolean isPlaying() {
        return phase == Phase.PLAYING;
    }

    public boolean isGameOver() {
        return phase == Phase.GAME_OVER;
    }
}
