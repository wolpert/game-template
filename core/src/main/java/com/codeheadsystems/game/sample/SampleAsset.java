package com.codeheadsystems.game.sample;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.codeheadsystems.game.assets.Asset;
import com.codeheadsystems.game.assets.LoadableAsset;

/**
 * Demo (falling-blocks sample) asset list. Mirrors {@link Asset} but contains only assets
 * specific to the bundled gameplay demo. Both enums implement {@link LoadableAsset} and are
 * aggregated into the loading set via Dagger {@code @ElementsIntoSet} bindings (scaffold
 * assets in {@link com.codeheadsystems.game.di.CoreModule}, sample assets in
 * {@link SampleModule}).
 */
public enum SampleAsset implements LoadableAsset {
    // Add new demo assets here.
    GAME_ATLAS("atlases/game-template.atlas", TextureAtlas.class);

    public final String path;
    public final Class<?> type;

    SampleAsset(String path, Class<?> type) {
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
