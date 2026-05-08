package com.codeheadsystems.game.ecs.system;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.codeheadsystems.game.ecs.component.AnimationComponent;
import com.codeheadsystems.game.ecs.component.PlayerComponent;
import com.codeheadsystems.game.session.GameState;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Drives the player's death sequence: when {@link GameState} enters {@code DYING}, swap the player's
 * animation to the one-shot Died animation; when that animation finishes, transition to {@code GAME_OVER}.
 * Runs before {@link AnimationSystem} so the swap takes effect on the same tick the phase changes.
 */
@Singleton
public class DeathSystem extends IteratingSystem {

    static final int PRIORITY = -6;
    private static final String DIED_REGION = "player1_Died";
    private static final float DIED_FRAME_DURATION = 0.1f;

    private final ComponentMapper<AnimationComponent> animations = ComponentMapper.getFor(AnimationComponent.class);
    private final GameState state;
    private final Animation<TextureRegion> diedAnimation;

    @Inject
    public DeathSystem(GameState state, TextureAtlas atlas) {
        super(Family.all(PlayerComponent.class, AnimationComponent.class).get(), PRIORITY);
        this.state = state;
        // PlayMode.NORMAL means isAnimationFinished() goes true after the last frame's duration.
        this.diedAnimation = new Animation<>(DIED_FRAME_DURATION, atlas.findRegions(DIED_REGION), Animation.PlayMode.NORMAL);
        if (diedAnimation.getKeyFrames().length == 0) {
            throw new IllegalStateException("Atlas missing region: " + DIED_REGION);
        }
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        if (state.phase != GameState.Phase.DYING) return;
        AnimationComponent anim = animations.get(entity);

        // First DYING tick: swap in the death animation and let AnimationSystem advance it from zero.
        if (anim.animation != diedAnimation) {
            anim.animation = diedAnimation;
            anim.elapsed = 0f;
            return;
        }

        if (diedAnimation.isAnimationFinished(anim.elapsed)) {
            state.phase = GameState.Phase.GAME_OVER;
        }
    }
}
