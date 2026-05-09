package com.codeheadsystems.game.sample;

import javax.inject.Inject;
import javax.inject.Singleton;

/** Mutable per-session state — reset by {@link SampleGameScreen} on entry. */
@Singleton
public class GameState {

    /**
     * Session lifecycle. PLAYING accepts input and spawns blocks; PAUSED freezes everything but
     * leaves the run resumable; DYING is the in-between where input/spawning are paused and the
     * death animation runs to completion; GAME_OVER signals {@link SampleGameScreen} to transition
     * to the game-over screen.
     *
     * <p>{@link #isPlaying()} returns true only for PLAYING — the {@code @Sample InputGate}
     * (bound to {@code state::isPlaying}) and {@link BlockSpawnSystem} both freeze on every other
     * phase, including PAUSED, for free.
     */
    public enum Phase { PLAYING, PAUSED, DYING, GAME_OVER }

    public static final int MAX_HP = 5;

    public int hp;
    public float elapsedSec;
    public Phase phase;
    /** Phase to restore on resume from PAUSED. Captured by {@link #pause()}. */
    private Phase resumePhase;

    @Inject
    public GameState() {
        reset();
    }

    public void reset() {
        hp = MAX_HP;
        elapsedSec = 0f;
        phase = Phase.PLAYING;
        resumePhase = Phase.PLAYING;
    }

    public boolean isPlaying() {
        return phase == Phase.PLAYING;
    }

    public boolean isPaused() {
        return phase == Phase.PAUSED;
    }

    public boolean isGameOver() {
        return phase == Phase.GAME_OVER;
    }

    /**
     * Pause is allowed only from PLAYING — pausing during DYING or GAME_OVER would be a sticky
     * state with no clear resume target. {@link SampleGameScreen} should check before calling.
     */
    public boolean canPause() {
        return phase == Phase.PLAYING;
    }

    /** Capture the current phase and switch to PAUSED. No-op if not currently pauseable. */
    public void pause() {
        if (!canPause()) return;
        resumePhase = phase;
        phase = Phase.PAUSED;
    }

    /** Resume to the captured pre-pause phase. No-op when not paused. */
    public void resume() {
        if (phase != Phase.PAUSED) return;
        phase = resumePhase;
    }
}
