package com.codeheadsystems.game.sample;

import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.codeheadsystems.game.assets.LoadableAsset;
import com.codeheadsystems.game.di.Sample;
import com.codeheadsystems.game.ecs.InputGate;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.ElementsIntoSet;
import dagger.multibindings.IntoSet;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Supplier;
import javax.inject.Singleton;

@Module
public abstract class SampleModule {

    @Provides @Sample @Singleton
    static InputGate provideSampleInputGate(GameState state) {
        return state::isPlaying;
    }

    @Provides @Sample @Singleton
    static com.badlogic.gdx.Screen provideSampleScreen(SampleGameScreen impl) {
        return impl;
    }

    @Provides @Sample @Singleton
    static Supplier<String> provideSampleDebugLine(GameState state) {
        return () -> state.phase.name();   // shows up in DebugOverlay's "phase: %s" slot
    }

    /**
     * Lifecycle foot-gun: throws {@code GdxRuntimeException} if {@link AssetManager} hasn't yet
     * finished loading {@link SampleAsset#GAME_ATLAS}. Callers must defer the lookup until
     * {@code LoadingScreen} has drained the manager — depend on {@code Provider<TextureAtlas>}
     * (or be transitively reachable only from a screen reached after loading), not on
     * {@code TextureAtlas} directly.
     */
    @Provides
    @Singleton
    static TextureAtlas provideTextureAtlas(AssetManager assets) {
        return assets.get(SampleAsset.GAME_ATLAS.path, TextureAtlas.class);
    }

    // Add new sample systems here:
    @Provides @Singleton @IntoSet
    static EntitySystem bindBlockSpawnSystem(BlockSpawnSystem s) { return s; }

    @Provides @Singleton @IntoSet
    static EntitySystem bindDeathSystem(DeathSystem s) { return s; }

    @Provides @Singleton @IntoSet
    static EntitySystem bindBlockCleanupSystem(BlockCleanupSystem s) { return s; }

    @Provides
    @ElementsIntoSet
    static Set<LoadableAsset> provideSampleAssets() {
        return Set.copyOf(EnumSet.allOf(SampleAsset.class));
    }
}
