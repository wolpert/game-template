package com.codeheadsystems.game.sample;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.codeheadsystems.game.config.GameConfig;
import com.codeheadsystems.game.ecs.component.AnimationComponent;
import com.codeheadsystems.game.ecs.component.BodyComponent;
import com.codeheadsystems.game.ecs.component.PositionComponent;
import com.codeheadsystems.game.ecs.component.TextureComponent;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Builds a single falling block (Box2D body + Ashley entity wired to it). The body's user data
 * is the entity itself, so the contact listener can look up components from a colliding body.
 */
@Singleton
public class FallingBlockFactory {

    private static final String BLOCK_REGION = "block_block";
    private static final float BLOCK_FRAME_DURATION = 0.12f;
    /** Slight horizontal jitter so spawns aren't all stacked on a single column. */
    private static final float SPAWN_TOP_MARGIN = 1.05f;

    private final World world;
    private final TextureAtlas atlas;
    private final GameConfig config;
    private final Graphics graphics;

    @Inject
    public FallingBlockFactory(World world, TextureAtlas atlas, GameConfig config, Graphics graphics) {
        this.world = world;
        this.atlas = atlas;
        this.config = config;
        this.graphics = graphics;
    }

    public Entity create() {
        Array<TextureRegion> frames = new Array<>(atlas.findRegions(BLOCK_REGION));
        if (frames.size == 0) {
            throw new IllegalStateException("Atlas is missing region: " + BLOCK_REGION);
        }
        TextureRegion firstFrame = frames.first();
        int blockW = firstFrame.getRegionWidth();
        int blockH = firstFrame.getRegionHeight();

        float ppm = config.physics.pixelsPerMeter;
        float screenW = graphics.getWidth();
        float screenH = graphics.getHeight();

        float spawnXPx = MathUtils.random(blockW / 2f, screenW - blockW / 2f);
        float spawnYPx = screenH * SPAWN_TOP_MARGIN; // just above the visible top

        BodyDef def = new BodyDef();
        def.type = BodyDef.BodyType.DynamicBody;
        def.position.set(spawnXPx / ppm, spawnYPx / ppm);
        // Random initial spin so off-center collisions look distinct from one another.
        def.angularVelocity = MathUtils.random(-2f, 2f);
        Body body = world.createBody(def);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox((blockW / 2f) / ppm, (blockH / 2f) / ppm);
        FixtureDef fixture = new FixtureDef();
        fixture.shape = shape;
        fixture.density = 1f;
        fixture.friction = 0.3f;
        fixture.restitution = 0.5f;
        body.createFixture(fixture);
        shape.dispose();

        Entity entity = new Entity();
        PositionComponent pos = new PositionComponent();
        pos.z = 2;
        entity.add(pos);
        TextureComponent tex = new TextureComponent();
        tex.region = firstFrame;
        entity.add(tex);
        AnimationComponent anim = new AnimationComponent();
        anim.animation = new Animation<>(BLOCK_FRAME_DURATION, frames, Animation.PlayMode.LOOP);
        entity.add(anim);
        BodyComponent bc = new BodyComponent();
        bc.body = body;
        entity.add(bc);
        entity.add(new BlockComponent());
        body.setUserData(entity);
        return entity;
    }
}
