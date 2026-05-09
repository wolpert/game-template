package com.codeheadsystems.game.ecs;

/**
 * Seam between the sample's {@code InputSystem} and the gameplay
 * lifecycle. The default scaffold binding (see {@code CoreModule.provideInputGate}) returns
 * {@code true} unconditionally, so input-driven entities always respond. Demo / sample
 * modules can rebind this via a {@code @Sample}-qualified provider (consumed through
 * {@code CoreModule}'s optional override slot) to a gameplay-state-aware impl (e.g. one that
 * returns {@code false} during DYING / GAME_OVER) without {@code InputSystem} taking a direct
 * dep on demo state.
 */
public interface InputGate {
    /** Return true if input-driven entities should respond to input this frame. */
    boolean isInputActive();
}
