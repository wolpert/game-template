package com.codeheadsystems.game.ecs.system;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.codeheadsystems.game.ecs.component.PositionComponent;
import com.codeheadsystems.game.ecs.component.TextureComponent;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class RenderSystemTest {

    @Test
    void wrapsEachUpdateInBeginEndAndDrawsMatchingEntities() {
        SpriteBatch batch = mock(SpriteBatch.class);
        Texture texture = mock(Texture.class);

        Engine engine = new PooledEngine();
        engine.addSystem(new RenderSystem(batch));

        engine.addEntity(makeEntity(texture, 10f, 20f));
        engine.addEntity(makeEntity(texture, 30f, 40f));
        engine.addEntity(makeEntityWithoutTexture(50f, 60f)); // should be ignored by the family

        engine.update(0.016f);

        InOrder order = inOrder(batch);
        order.verify(batch).begin();
        order.verify(batch).draw(texture, 10f, 20f);
        order.verify(batch).draw(texture, 30f, 40f);
        order.verify(batch).end();
        order.verifyNoMoreInteractions();
    }

    private static Entity makeEntity(Texture texture, float x, float y) {
        Entity entity = new Entity();
        PositionComponent pos = new PositionComponent();
        pos.x = x;
        pos.y = y;
        TextureComponent tex = new TextureComponent();
        tex.texture = texture;
        entity.add(pos);
        entity.add(tex);
        return entity;
    }

    private static Entity makeEntityWithoutTexture(float x, float y) {
        Entity entity = new Entity();
        PositionComponent pos = new PositionComponent();
        pos.x = x;
        pos.y = y;
        entity.add(pos);
        return entity;
    }
}
