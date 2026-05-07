package com.codeheadsystems.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.codeheadsystems.game.di.DaggerGameComponent;
import com.codeheadsystems.game.screens.ScreenNavigator;
import javax.inject.Inject;

/**
 * Entry point — builds the Dagger graph and hands control to {@link ScreenNavigator}, which
 * drives all screen transitions. The persistent resources (batch, textures, world, skin) live
 * here for app-lifetime ownership; per-screen state (Stages) lives on the screens themselves.
 */
public class TheGame extends Game {

    @Inject SpriteBatch batch;
    @Inject Texture image;
    @Inject TextureAtlas atlas;
    @Inject World world;
    @Inject Skin skin;
    @Inject ScreenNavigator nav;

    @Override
    public void create() {
        // Providers may touch GL/Gdx, which is only valid after create() — so build here.
        DaggerGameComponent.builder().game(this).build().inject(this);
        nav.goToLoading();
    }

    @Override
    public void dispose() {
        Screen current = getScreen();
        if (current != null) {
            current.hide();
        }
        nav.disposeAll();
        batch.dispose();
        image.dispose();
        atlas.dispose();
        world.dispose();
        skin.dispose();
    }
}
