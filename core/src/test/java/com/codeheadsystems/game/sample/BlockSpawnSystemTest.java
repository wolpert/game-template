package com.codeheadsystems.game.sample;

import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.Graphics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BlockSpawnSystemTest {

    private FallingBlockFactory factory;
    private GameState state;
    private Graphics graphics;
    private Engine engine;
    private BlockSpawnSystem system;

    @BeforeEach
    void setUp() {
        factory = mock(FallingBlockFactory.class);
        when(factory.create(anyFloat(), anyFloat())).thenAnswer(inv -> new Entity());
        state = new GameState();
        graphics = mock(Graphics.class);
        when(graphics.getWidth()).thenReturn(800);
        when(graphics.getHeight()).thenReturn(600);
        system = new BlockSpawnSystem(factory, state, graphics);
        engine = new PooledEngine();
        engine.addSystem(system);
    }

    @Test
    void spawnsImmediatelyOnFirstUpdate() {
        engine.update(0.016f);

        verify(factory, times(1)).create(anyFloat(), anyFloat());
    }

    @Test
    void waitsForIntervalBetweenSpawns() {
        engine.update(0.016f);
        verify(factory, times(1)).create(anyFloat(), anyFloat());

        // Just under the interval — no new spawn.
        engine.update(BlockSpawnSystem.SPAWN_INTERVAL - 0.1f);
        verify(factory, times(1)).create(anyFloat(), anyFloat());

        // Cross the threshold — second spawn.
        engine.update(0.2f);
        verify(factory, times(2)).create(anyFloat(), anyFloat());
    }

    @Test
    void doesNotSpawnWhenNotPlaying() {
        state.phase = GameState.Phase.DYING; // any non-PLAYING phase suffices

        engine.update(5f); // far beyond an interval

        verify(factory, never()).create(anyFloat(), anyFloat());
    }

    @Test
    void resetSchedulesAnImmediateNextSpawn() {
        engine.update(0.016f);
        verify(factory, times(1)).create(anyFloat(), anyFloat());

        engine.update(0.5f); // partway through, no second spawn yet
        verify(factory, times(1)).create(anyFloat(), anyFloat());

        system.reset();
        engine.update(0.016f);

        verify(factory, times(2)).create(anyFloat(), anyFloat());
    }
}
