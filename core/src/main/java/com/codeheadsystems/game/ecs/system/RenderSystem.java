package com.codeheadsystems.game.ecs.system;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.codeheadsystems.game.ecs.component.PositionComponent;
import com.codeheadsystems.game.ecs.component.TextureComponent;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RenderSystem extends IteratingSystem {

    private final SpriteBatch batch;
    private final ComponentMapper<PositionComponent> positions = ComponentMapper.getFor(PositionComponent.class);
    private final ComponentMapper<TextureComponent> textures = ComponentMapper.getFor(TextureComponent.class);

    @Inject
    public RenderSystem(SpriteBatch batch) {
        super(Family.all(PositionComponent.class, TextureComponent.class).get());
        this.batch = batch;
    }

    @Override
    public void update(float deltaTime) {
        batch.begin();
        super.update(deltaTime);
        batch.end();
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        PositionComponent pos = positions.get(entity);
        TextureComponent tex = textures.get(entity);
        batch.draw(tex.texture, pos.x, pos.y);
    }
}
