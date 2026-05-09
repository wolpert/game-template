package com.codeheadsystems.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.codeheadsystems.game.di.DaggerGameComponent;
import com.codeheadsystems.game.lifecycle.AppLifecycle;
import com.codeheadsystems.game.physics.PhysicsWorld;
import com.codeheadsystems.game.screens.ScreenNavigator;
import javax.inject.Inject;

/**
 * Entry point — builds the Dagger graph and hands control to {@link ScreenNavigator}, which
 * drives all screen transitions. The persistent resources (batch, asset manager, world, skin)
 * live here for app-lifetime ownership; per-screen state (Stages) lives on the screens themselves.
 *
 * <p>{@link AssetManager} owns every {@link com.badlogic.gdx.graphics.Texture} and
 * {@link com.badlogic.gdx.graphics.g2d.TextureAtlas} loaded by the game, so disposing it
 * releases the lot — no need to track those individually here.
 */
public class TheGame extends Game {

    @Inject SpriteBatch batch;
    @Inject AssetManager assets;
    @Inject PhysicsWorld physicsWorld;
    @Inject Skin skin;
    @Inject ScreenNavigator nav;
    @Inject AppLifecycle appLifecycle;

    @Override
    public void create() {
        // Providers may touch GL/Gdx, which is only valid after create() — so build here.
        DaggerGameComponent.builder().game(this).build().inject(this);
        nav.goToLoading();
    }

    @Override
    public void pause() {
        // Android activity onPause / desktop window minimize. Flip the lifecycle flag BEFORE
        // delegating so any screen.pause() callback below already sees the gate as inactive.
        appLifecycle.setActive(false);
        super.pause();
    }

    @Override
    public void resume() {
        // Foregrounded again — re-open the gate. Screens decide independently whether to auto-
        // resume gameplay; we just signal "the JVM is live again".
        appLifecycle.setActive(true);
        super.resume();
    }

    @Override
    public void dispose() {
        Screen current = getScreen();
        if (current != null) {
            current.hide();
        }
        nav.disposeAll();
        batch.dispose();
        assets.dispose();
        physicsWorld.dispose();
        skin.dispose();
    }
}
