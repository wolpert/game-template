package com.codeheadsystems.game.ecs.system;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.PooledEngine;
import com.codeheadsystems.game.session.FallingBlockFactory;
import com.codeheadsystems.game.session.GameState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BlockSpawnSystemTest {

    private FallingBlockFactory factory;
    private GameState state;
    private Engine engine;
    private BlockSpawnSystem system;

    @BeforeEach
    void setUp() {
        factory = mock(FallingBlockFactory.class);
        when(factory.create()).thenAnswer(inv -> new Entity());
        state = new GameState();
        system = new BlockSpawnSystem(factory, state);
        engine = new PooledEngine();
        engine.addSystem(system);
    }

    @Test
    void spawnsImmediatelyOnFirstUpdate() {
        engine.update(0.016f);

        verify(factory, times(1)).create();
    }

    @Test
    void waitsForIntervalBetweenSpawns() {
        engine.update(0.016f);
        verify(factory, times(1)).create();

        // Just under the interval — no new spawn.
        engine.update(BlockSpawnSystem.SPAWN_INTERVAL - 0.1f);
        verify(factory, times(1)).create();

        // Cross the threshold — second spawn.
        engine.update(0.2f);
        verify(factory, times(2)).create();
    }

    @Test
    void doesNotSpawnWhenGameOver() {
        state.gameOver = true;

        engine.update(5f); // far beyond an interval

        verify(factory, never()).create();
    }

    @Test
    void resetSchedulesAnImmediateNextSpawn() {
        engine.update(0.016f);
        verify(factory, times(1)).create();

        engine.update(0.5f); // partway through, no second spawn yet
        verify(factory, times(1)).create();

        system.reset();
        engine.update(0.016f);

        verify(factory, times(2)).create();
    }
}
