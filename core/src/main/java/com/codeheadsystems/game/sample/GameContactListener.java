package com.codeheadsystems.game.sample;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.codeheadsystems.game.ecs.component.InputComponent;
import com.codeheadsystems.game.render.CameraShake;
import com.codeheadsystems.game.render.Hitstop;
import com.codeheadsystems.game.render.TintFlash;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Box2D contact listener that ends the session on player↔block collision. Looks up the Ashley
 * entity via {@code Body.getUserData()} (set by the factories that create those bodies). Also
 * triggers hit feedback on each player↔block hit: short camera shake, a tiny hitstop, and a
 * red tint flash on the player sprite. The render utilities live in scaffold so any future game
 * can reuse them; this listener is just the sample-side trigger point.
 */
@Singleton
public class GameContactListener implements ContactListener {

    /** Magnitude of the per-hit camera shake, in pixels. Big enough to read, small enough to not nauseate. */
    private static final float SHAKE_MAGNITUDE_PX = 8f;
    private static final float SHAKE_DURATION_SEC = 0.15f;
    /** ~60ms freeze frame — the canonical "juicy hit" feel. */
    private static final float HITSTOP_DURATION_SEC = 0.06f;
    private static final float TINT_FLASH_DURATION_SEC = 0.15f;

    private final ComponentMapper<InputComponent> inputs = ComponentMapper.getFor(InputComponent.class);
    private final ComponentMapper<BlockComponent> blocks = ComponentMapper.getFor(BlockComponent.class);
    private final GameState state;
    private final CameraShake cameraShake;
    private final Hitstop hitstop;
    private final TintFlash tintFlash;

    @Inject
    public GameContactListener(GameState state,
                               CameraShake cameraShake,
                               Hitstop hitstop,
                               TintFlash tintFlash) {
        this.state = state;
        this.cameraShake = cameraShake;
        this.hitstop = hitstop;
        this.tintFlash = tintFlash;
    }

    @Override
    public void beginContact(Contact contact) {
        if (!state.isPlaying()) return; // ignore late contacts during PAUSED / DYING / GAME_OVER

        Object aData = contact.getFixtureA().getBody().getUserData();
        Object bData = contact.getFixtureB().getBody().getUserData();
        if (!(aData instanceof Entity) || !(bData instanceof Entity)) return;
        Entity a = (Entity) aData;
        Entity b = (Entity) bData;

        Entity playerEntity = null;
        if (inputs.has(a) && blocks.has(b)) playerEntity = a;
        else if (inputs.has(b) && blocks.has(a)) playerEntity = b;
        if (playerEntity == null) return;

        state.hp--;
        // Hit feedback fires on every player↔block contact, even the lethal one — the brief
        // freeze and shake reinforce the impact before the death animation kicks in.
        cameraShake.trigger(SHAKE_MAGNITUDE_PX, SHAKE_DURATION_SEC);
        hitstop.freeze(HITSTOP_DURATION_SEC);
        tintFlash.flash(playerEntity, Color.RED, TINT_FLASH_DURATION_SEC);

        if (state.hp <= 0) {
            state.phase = GameState.Phase.DYING;
        }
    }

    @Override public void endContact(Contact contact) {}
    @Override public void preSolve(Contact contact, Manifold oldManifold) {}
    @Override public void postSolve(Contact contact, ContactImpulse impulse) {}
}
