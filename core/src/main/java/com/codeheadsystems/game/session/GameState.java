package com.codeheadsystems.game.session;

import javax.inject.Inject;
import javax.inject.Singleton;

/** Mutable per-session state — reset by {@link com.codeheadsystems.game.screens.GameScreen} on entry. */
@Singleton
public class GameState {
    public boolean gameOver;
    public float elapsedSec;

    @Inject
    public GameState() {}

    public void reset() {
        gameOver = false;
        elapsedSec = 0f;
    }
}
