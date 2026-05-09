package com.codeheadsystems.game.sample;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GameStateTest {

    private GameState state;

    @BeforeEach
    void setUp() {
        state = new GameState();
    }

    @Test
    void initialPhaseIsPlaying() {
        assertEquals(GameState.Phase.PLAYING, state.phase);
        assertTrue(state.isPlaying());
        assertFalse(state.isPaused());
        assertFalse(state.isGameOver());
        assertEquals(GameState.MAX_HP, state.hp);
    }

    @Test
    void pauseTransitionsFromPlayingToPausedAndResumeReturns() {
        assertTrue(state.canPause());
        state.pause();
        assertEquals(GameState.Phase.PAUSED, state.phase);
        assertFalse(state.isPlaying(), "isPlaying must be false while paused so InputGate freezes player");
        assertTrue(state.isPaused());

        state.resume();
        assertEquals(GameState.Phase.PLAYING, state.phase);
        assertTrue(state.isPlaying());
    }

    @Test
    void pauseDuringDyingIsBlocked() {
        state.phase = GameState.Phase.DYING;
        assertFalse(state.canPause());
        state.pause();
        assertEquals(GameState.Phase.DYING, state.phase, "DYING is sticky — pause must not override");
    }

    @Test
    void pauseDuringGameOverIsBlocked() {
        state.phase = GameState.Phase.GAME_OVER;
        assertFalse(state.canPause());
        state.pause();
        assertEquals(GameState.Phase.GAME_OVER, state.phase);
    }

    @Test
    void resumeWhenNotPausedIsNoOp() {
        state.phase = GameState.Phase.DYING;
        state.resume();
        assertEquals(GameState.Phase.DYING, state.phase);
    }

    @Test
    void resetClearsPauseAndState() {
        state.pause();
        state.elapsedSec = 5f;
        state.hp = 0;

        state.reset();

        assertEquals(GameState.Phase.PLAYING, state.phase);
        assertEquals(GameState.MAX_HP, state.hp);
        assertEquals(0f, state.elapsedSec, 0f);
    }
}
