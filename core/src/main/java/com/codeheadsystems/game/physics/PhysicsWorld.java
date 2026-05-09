package com.codeheadsystems.game.physics;

import com.badlogic.ashley.core.Engine;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Box2D;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.codeheadsystems.game.config.GameConfig;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Owns the Box2D {@link World} and its {@link ContactListener} as a single unit so screens (and
 * {@link com.codeheadsystems.game.TheGame}) don't have to coordinate the world + listener pair
 * across the scaffold/sample seam. Provides {@link #getWorld()} for raw-world consumers
 * ({@link com.codeheadsystems.game.ecs.system.PhysicsSystem}, factories), a session-reset hook
 * ({@link #clearSession(Engine)}), and a single {@link #dispose()}.
 *
 * <p><strong>Lifecycle:</strong> constructed eagerly inside the Dagger graph, after
 * {@link Box2D#init()} runs. Disposed once, by {@code TheGame.dispose()}.
 *
 * <p><strong>Why this class exists:</strong> the contact listener belongs to gameplay
 * (the dodge sample's {@code GameContactListener}) but the {@code World} belongs to scaffold —
 * this wrapper lets the sample install/replace its listener without scaffold importing anything
 * sample-specific, and lets {@code TheGame} dispose physics through one handle.
 */
@Singleton
public class PhysicsWorld implements Disposable {

    private final World world;
    /** Reused across {@link #clearSession(Engine)} calls to avoid per-restart allocation. */
    private final Array<Body> bodiesScratch = new Array<>();

    @Inject
    public PhysicsWorld(GameConfig config) {
        Box2D.init(); // idempotent; safer than relying on lazy native loading.
        this.world = new World(
                new Vector2(config.physics.gravity.x, config.physics.gravity.y),
                /*doSleep=*/ true);
    }

    /**
     * Raw-world accessor for systems and factories that need the libGDX {@link World} type
     * directly (stepping, body creation, body-count queries).
     */
    public World getWorld() {
        return world;
    }

    /**
     * Replaces the currently-installed {@link ContactListener} (or installs the first one).
     * Idempotent — safe to call on every session start.
     */
    public void setContactListener(ContactListener listener) {
        world.setContactListener(listener);
    }

    /**
     * Tears down the current session in a single, well-defined order:
     * <ol>
     *   <li>Destroy every {@link Body} in the {@link World}. Bodies hold the Ashley
     *       {@link com.badlogic.ashley.core.Entity} reference as {@code userData}, so we must do
     *       this <em>before</em> the engine drops them — otherwise Box2D's destruction callbacks
     *       (and the contact listener) would fire on bodies whose {@code userData} pointed at
     *       entities the engine has already detached and recycled. Destroying bodies first means
     *       the {@code Entity} userData is still attached and well-formed for any in-flight
     *       contact end-events fired during destruction.</li>
     *   <li>{@link Engine#removeAllEntities()} clears every entity from Ashley. After this point
     *       no {@code BodyComponent} references can dangle, because their bodies are already gone.</li>
     * </ol>
     *
     * <p>Idempotent: calling on an already-empty session is a no-op (no bodies to iterate, no
     * entities to remove).
     */
    public void clearSession(Engine engine) {
        bodiesScratch.clear();
        world.getBodies(bodiesScratch);
        for (Body b : bodiesScratch) {
            world.destroyBody(b);
        }
        bodiesScratch.clear();
        engine.removeAllEntities();
    }

    @Override
    public void dispose() {
        world.dispose();
    }
}
