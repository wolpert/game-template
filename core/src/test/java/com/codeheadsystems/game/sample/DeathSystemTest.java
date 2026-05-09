package com.codeheadsystems.game.sample;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.codeheadsystems.game.ecs.component.AnimationComponent;
import com.codeheadsystems.game.ecs.component.TextureComponent;
import com.codeheadsystems.game.ecs.system.AnimationSystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeathSystemTest {

    private static final int DIED_FRAME_COUNT = 8;

    private GameState state;
    private AnimationComponent anim;
    private Animation<TextureRegion> originalAnimation;
    private Engine engine;

    @BeforeEach
    void setUp() {
        state = new GameState();

        TextureAtlas atlas = mock(TextureAtlas.class);
        // Array must be constructed with the element class so libGDX's Animation can extract a
        // properly-typed underlying array (default Array<>() backs by Object[], which fails the cast).
        Array<AtlasRegion> diedFrames = new Array<>(AtlasRegion.class);
        for (int i = 0; i < DIED_FRAME_COUNT; i++) {
            diedFrames.add(mock(AtlasRegion.class));
        }
        when(atlas.findRegions("player1_Died")).thenReturn(diedFrames);

        // Player initially has its looping flying animation; DeathSystem should swap it out on DYING.
        Array<TextureRegion> flyingFrames = new Array<>(TextureRegion.class);
        flyingFrames.add(mock(TextureRegion.class));
        flyingFrames.add(mock(TextureRegion.class));
        originalAnimation = new Animation<>(0.1f, flyingFrames, Animation.PlayMode.LOOP);

        anim = new AnimationComponent();
        anim.animation = originalAnimation;
        anim.elapsed = 5f; // simulate mid-flight at the moment of death

        Entity player = new Entity();
        player.add(new PlayerComponent());
        player.add(anim);
        player.add(new TextureComponent());

        engine = new PooledEngine();
        // AnimationSystem advances elapsed; DeathSystem (-6) runs before AnimationSystem (0) so the
        // swap happens before elapsed is bumped.
        engine.addSystem(new DeathSystem(state, atlas));
        engine.addSystem(new AnimationSystem());
        engine.addEntity(player);
    }

    @Test
    void doesNothingWhilePlaying() {
        engine.update(0.5f);

        assertSame(originalAnimation, anim.animation);
        assertEquals(GameState.Phase.PLAYING, state.phase);
    }

    @Test
    void swapsToDiedAnimationOnFirstDyingTickAndResetsElapsed() {
        state.phase = GameState.Phase.DYING;

        engine.update(0.016f);

        assertNotSame(originalAnimation, anim.animation, "expected animation to be swapped to Died");
        // First tick: DeathSystem swapped to elapsed=0, AnimationSystem then advanced by dt.
        assertEquals(0.016f, anim.elapsed, 1e-6);
        assertEquals(GameState.Phase.DYING, state.phase, "still dying — animation hasn't finished yet");
    }

    @Test
    void transitionsToGameOverWhenDiedAnimationFinishes() {
        state.phase = GameState.Phase.DYING;
        engine.update(0.016f); // swap to Died, elapsed = 0.016

        // Total Died duration: DIED_FRAME_COUNT * 0.1 = 0.8s. Step well past it.
        for (int i = 0; i < 60; i++) {
            engine.update(0.016f);
        }

        assertEquals(GameState.Phase.GAME_OVER, state.phase);
    }

    @Test
    void doesNotTransitionEarly() {
        state.phase = GameState.Phase.DYING;
        engine.update(0.016f); // swap

        // Just under the Died duration (8 frames * 0.1s = 0.8s).
        for (int i = 0; i < 40; i++) {
            engine.update(0.016f); // total elapsed ≈ 0.016 + 40*0.016 ≈ 0.66s
        }

        assertEquals(GameState.Phase.DYING, state.phase);
    }
}
