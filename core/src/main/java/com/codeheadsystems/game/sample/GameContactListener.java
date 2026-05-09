package com.codeheadsystems.game.sample;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.codeheadsystems.game.ecs.component.InputComponent;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Box2D contact listener that ends the session on player↔block collision. Looks up the Ashley
 * entity via {@code Body.getUserData()} (set by the factories that create those bodies).
 */
@Singleton
public class GameContactListener implements ContactListener {

    private final ComponentMapper<InputComponent> inputs = ComponentMapper.getFor(InputComponent.class);
    private final ComponentMapper<BlockComponent> blocks = ComponentMapper.getFor(BlockComponent.class);
    private final GameState state;

    @Inject
    public GameContactListener(GameState state) {
        this.state = state;
    }

    @Override
    public void beginContact(Contact contact) {
        if (!state.isPlaying()) return; // ignore late contacts during DYING / GAME_OVER

        Object aData = contact.getFixtureA().getBody().getUserData();
        Object bData = contact.getFixtureB().getBody().getUserData();
        if (!(aData instanceof Entity) || !(bData instanceof Entity)) return;
        Entity a = (Entity) aData;
        Entity b = (Entity) bData;

        boolean playerHitsBlock = (inputs.has(a) && blocks.has(b))
                || (inputs.has(b) && blocks.has(a));
        if (playerHitsBlock) {
            state.hp--;
            if (state.hp <= 0) {
                state.phase = GameState.Phase.DYING;
            }
        }
    }

    @Override public void endContact(Contact contact) {}
    @Override public void preSolve(Contact contact, Manifold oldManifold) {}
    @Override public void postSolve(Contact contact, ContactImpulse impulse) {}
}
