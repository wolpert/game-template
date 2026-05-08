package com.codeheadsystems.game.ecs.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.Graphics;
import com.codeheadsystems.game.ecs.component.PositionComponent;
import com.codeheadsystems.game.ecs.component.WrapAroundComponent;
import org.junit.jupiter.api.Test;

class WrapAroundSystemTest {

    private static final float SCREEN_WIDTH = 800f;
    private static final float SPRITE_WIDTH = 100f;

    private Engine engineWithSystem() {
        Graphics graphics = mock(Graphics.class);
        when(graphics.getWidth()).thenReturn((int) SCREEN_WIDTH);
        Engine engine = new PooledEngine();
        engine.addSystem(new WrapAroundSystem(graphics));
        return engine;
    }

    private Entity drifter(float x) {
        PositionComponent pos = new PositionComponent();
        pos.x = x;
        WrapAroundComponent wrap = new WrapAroundComponent();
        wrap.widthPx = SPRITE_WIDTH;
        Entity e = new Entity();
        e.add(pos);
        e.add(wrap);
        return e;
    }

    @Test
    void wrapsToLeftWhenLeftEdgePassesScreenRight() {
        Engine engine = engineWithSystem();
        // pos.x is bottom-left; the left edge passing the right edge means pos.x > screenW.
        Entity entity = drifter(SCREEN_WIDTH + 5f);
        engine.addEntity(entity);

        engine.update(0f);

        assertEquals(-SPRITE_WIDTH, entity.getComponent(PositionComponent.class).x, 1e-6);
    }

    @Test
    void wrapsToRightWhenRightEdgePassesScreenLeft() {
        Engine engine = engineWithSystem();
        // Right edge = pos.x + width; off-screen-left means pos.x + width < 0.
        Entity entity = drifter(-SPRITE_WIDTH - 1f);
        engine.addEntity(entity);

        engine.update(0f);

        assertEquals(SCREEN_WIDTH, entity.getComponent(PositionComponent.class).x, 1e-6);
    }

    @Test
    void leavesOnscreenEntityAlone() {
        Engine engine = engineWithSystem();
        Entity entity = drifter(200f);
        engine.addEntity(entity);

        engine.update(0f);

        assertEquals(200f, entity.getComponent(PositionComponent.class).x, 1e-6);
    }
}
