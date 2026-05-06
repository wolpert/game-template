package com.codeheadsystems.game.ecs.system;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.SortedIteratingSystem;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.codeheadsystems.game.ecs.component.PositionComponent;
import com.codeheadsystems.game.ecs.component.TextureComponent;
import java.util.Comparator;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RenderSystem extends SortedIteratingSystem {

    static final int PRIORITY = 10;

    private static final ComponentMapper<PositionComponent> POSITIONS = ComponentMapper.getFor(PositionComponent.class);
    private static final ComponentMapper<TextureComponent> TEXTURES = ComponentMapper.getFor(TextureComponent.class);
    private static final Comparator<Entity> BY_Z =
            (a, b) -> Integer.compare(POSITIONS.get(a).z, POSITIONS.get(b).z);

    private final SpriteBatch batch;

    @Inject
    public RenderSystem(SpriteBatch batch) {
        super(Family.all(PositionComponent.class, TextureComponent.class).get(), BY_Z, PRIORITY);
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
        PositionComponent pos = POSITIONS.get(entity);
        TextureComponent tex = TEXTURES.get(entity);
        batch.draw(tex.region, pos.x, pos.y);
    }
}
