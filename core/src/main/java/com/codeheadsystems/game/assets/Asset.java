package com.codeheadsystems.game.assets;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;

/**
 * Single source of truth for assets queued through {@link com.badlogic.gdx.assets.AssetManager}.
 * Each constant is a {@code (path, type)} pair. {@link com.codeheadsystems.game.screens.LoadingScreen}
 * iterates {@link #values()} to queue loads; {@link com.codeheadsystems.game.di.GameModule}
 * resolves each entry by path when wiring providers.
 *
 * <p>Adding an asset = add a constant here, drop the file under {@code assets/}, then run
 * {@code :core:processResources} so {@code assets.txt} regenerates and the
 * {@link AssetManifest} validation passes.
 *
 * <p>Out of scope: assets loaded outside the manager (the libGDX {@link com.badlogic.gdx.scenes.scene2d.ui.Skin}
 * and {@code config/game.yaml} are loaded eagerly elsewhere because {@link com.codeheadsystems.game.screens.LoadingScreen}
 * itself needs them before the manager has run).
 */
public enum Asset {
    LOGO("libgdx.png", Texture.class),
    GAME_ATLAS("atlases/game-template.atlas", TextureAtlas.class);

    public final String path;
    public final Class<?> type;

    Asset(String path, Class<?> type) {
        this.path = path;
        this.type = type;
    }

    @SuppressWarnings("unchecked")
    public <T> Class<T> typedClass() {
        return (Class<T>) type;
    }
}
