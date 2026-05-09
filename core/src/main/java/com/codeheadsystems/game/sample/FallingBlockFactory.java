package com.codeheadsystems.game.sample;

import com.badlogic.ashley.core.Engine;
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
import com.badlogic.gdx.utils.Disposable;
import com.codeheadsystems.game.config.GameConfig;
import com.codeheadsystems.game.ecs.component.AnimationComponent;
import com.codeheadsystems.game.ecs.component.BodyComponent;
import com.codeheadsystems.game.ecs.component.PositionComponent;
import com.codeheadsystems.game.ecs.component.TextureComponent;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * Builds a single falling block (Box2D body + Ashley entity wired to it). The body's user data
 * is the entity itself, so the contact listener can look up components from a colliding body.
 *
 * <p>Per-spawn allocation is hot-path: this factory caches the region array, the
 * {@link Animation}, and the reusable Box2D builder structs so each {@link #create()} call
 * mutates rather than allocates. Box2D copies the {@link BodyDef}/{@link FixtureDef} fields
 * out at body/fixture creation time, so it's safe to reuse the same instances for every spawn.
 *
 * <p>{@link PolygonShape} holds a native handle and must be disposed at app exit; see
 * {@link #dispose()}. The factory is {@code @Singleton}, so the shape lives for the whole app
 * lifetime — leaking it at process exit would be reclaimed by the OS, but {@link #dispose()}
 * is provided for callers that prefer explicit cleanup. Wiring it to {@code TheGame.dispose()}
 * would force scaffold to import sample, so it's left optional.
 */
@Singleton
public class FallingBlockFactory implements Disposable {

    private static final String BLOCK_REGION = "block_block";
    private static final float BLOCK_FRAME_DURATION = 0.12f;
    /** Slight horizontal jitter so spawns aren't all stacked on a single column. */
    private static final float SPAWN_TOP_MARGIN = 1.05f;

    private final World world;
    private final GameConfig config;
    private final Graphics graphics;
    /**
     * {@code Provider} (not direct injection) breaks the Dagger cycle: {@code Engine} depends on the
     * {@code Set<EntitySystem>}, which transitively depends on this factory through
     * {@link BlockSpawnSystem}. The provider is resolved lazily inside {@link #create()}, by which
     * time the engine has been built.
     */
    private final Provider<Engine> engineProvider;

    // Cached at construction — atlas regions and the Animation are fixed for the app lifetime.
    private final Array<TextureRegion> frames;
    private final TextureRegion firstFrame;
    private final int blockW;
    private final int blockH;
    private final Animation<TextureRegion> animation;

    // Reusable Box2D builders. Box2D copies their fields when creating bodies/fixtures, so we mutate
    // them between calls. The PolygonShape holds a native handle and is disposed in dispose().
    private final BodyDef bodyDef = new BodyDef();
    private final FixtureDef fixtureDef = new FixtureDef();
    private final PolygonShape shape = new PolygonShape();

    @Inject
    public FallingBlockFactory(World world, TextureAtlas atlas, GameConfig config, Graphics graphics, Provider<Engine> engineProvider) {
        this.world = world;
        this.config = config;
        this.graphics = graphics;
        this.engineProvider = engineProvider;

        Array<TextureRegion> regions = new Array<>(atlas.findRegions(BLOCK_REGION));
        if (regions.size == 0) {
            throw new IllegalStateException("Atlas is missing region: " + BLOCK_REGION);
        }
        this.frames = regions;
        this.firstFrame = regions.first();
        this.blockW = firstFrame.getRegionWidth();
        this.blockH = firstFrame.getRegionHeight();
        this.animation = new Animation<>(BLOCK_FRAME_DURATION, frames, Animation.PlayMode.LOOP);

        // Shape is sized once and reused for every block spawn.
        float ppm = config.physics.pixelsPerMeter;
        shape.setAsBox((blockW / 2f) / ppm, (blockH / 2f) / ppm);

        // Static fixture properties — only the shape reference and per-spawn position need to vary.
        fixtureDef.shape = shape;
        fixtureDef.density = 1f;
        fixtureDef.friction = 0.3f;
        fixtureDef.restitution = 0.5f;

        bodyDef.type = BodyDef.BodyType.DynamicBody;
    }

    public Entity create() {
        Engine engine = engineProvider.get();
        float ppm = config.physics.pixelsPerMeter;
        float screenW = graphics.getWidth();
        float screenH = graphics.getHeight();

        float spawnXPx = MathUtils.random(blockW / 2f, screenW - blockW / 2f);
        float spawnYPx = screenH * SPAWN_TOP_MARGIN; // just above the visible top

        bodyDef.position.set(spawnXPx / ppm, spawnYPx / ppm);
        // Random initial spin so off-center collisions look distinct from one another.
        bodyDef.angularVelocity = MathUtils.random(-2f, 2f);
        Body body = world.createBody(bodyDef);
        body.createFixture(fixtureDef);

        Entity entity = engine.createEntity();
        PositionComponent pos = engine.createComponent(PositionComponent.class);
        // Note: createComponent is on Engine in Ashley 1.7.4 (delegates to PooledEngine subclass).
        pos.z = 2;
        entity.add(pos);
        TextureComponent tex = engine.createComponent(TextureComponent.class);
        tex.region = firstFrame;
        entity.add(tex);
        AnimationComponent anim = engine.createComponent(AnimationComponent.class);
        anim.animation = animation;
        entity.add(anim);
        BodyComponent bc = engine.createComponent(BodyComponent.class);
        bc.body = body;
        entity.add(bc);
        entity.add(engine.createComponent(BlockComponent.class));
        body.setUserData(entity);
        return entity;
    }

    /**
     * Releases the cached {@link PolygonShape}'s native handle. Optional — the OS reclaims it at
     * process exit. Wire to a lifecycle hook only if scaffold is willing to import sample (which
     * it currently isn't).
     */
    @Override
    public void dispose() {
        shape.dispose();
    }
}
