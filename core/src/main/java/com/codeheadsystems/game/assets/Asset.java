package com.codeheadsystems.game.assets;

import com.badlogic.gdx.graphics.Texture;

/**
 * Scaffold asset list — assets that ship with the bare template regardless of demo content.
 * Each constant is a {@code (path, type)} pair. {@link com.codeheadsystems.game.screens.LoadingScreen}
 * iterates a {@code Set<LoadableAsset>} (multi-bound in
 * {@link com.codeheadsystems.game.di.CoreModule}) to queue loads;
 * {@link com.codeheadsystems.game.di.CoreModule} resolves entries by path when wiring providers.
 *
 * <p>Adding an asset = add a constant here, drop the file under {@code assets/}, then run
 * {@code :core:processResources} so {@code assets.txt} regenerates and the
 * {@link AssetManifest} validation passes.
 *
 * <p>Demo-specific assets live in {@link com.codeheadsystems.game.sample.SampleAsset}; both enums implement {@link LoadableAsset}
 * and are aggregated into the loading set via Dagger {@code @ElementsIntoSet} bindings.
 *
 * <p>Out of scope: assets loaded outside the manager (the libGDX {@link com.badlogic.gdx.scenes.scene2d.ui.Skin}
 * and {@code config/game.yaml} are loaded eagerly elsewhere because {@link com.codeheadsystems.game.screens.LoadingScreen}
 * itself needs them before the manager has run).
 */
public enum Asset implements LoadableAsset {
    // Add new scaffold assets here. Demo-only assets go in SampleAsset.
    LOGO("libgdx.png", Texture.class);

    public final String path;
    public final Class<?> type;

    Asset(String path, Class<?> type) {
        this.path = path;
        this.type = type;
    }

    @Override
    public String path() {
        return path;
    }

    @Override
    public Class<?> type() {
        return type;
    }

    @SuppressWarnings("unchecked")
    public <T> Class<T> typedClass() {
        return (Class<T>) type;
    }
}
