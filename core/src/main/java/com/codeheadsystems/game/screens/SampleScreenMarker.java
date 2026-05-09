package com.codeheadsystems.game.screens;

import com.badlogic.gdx.Screen;

/**
 * Scaffold-side {@link Screen} marker class used as the registry key for the optional sample
 * (dodge demo) screen. The scaffold cannot import {@code com.codeheadsystems.game.sample.*}, so
 * this marker is the stable handle {@link ScreenNavigator#has(Class)} and
 * {@link ScreenNavigator#goTo(Class)} use when the demo's actual {@code SampleGameScreen} is on
 * the classpath.
 *
 * <p>When {@code SampleModule} is wired in, it contributes a
 * {@code @Provides @IntoMap @ScreenKey(SampleScreenMarker.class) Screen} entry that returns the
 * real {@code SampleGameScreen}. When the sample is stripped, no entry exists for this key, the
 * navigator's lookup misses, and {@code LevelPickerScreen} hides the "Dodge Sample" button.
 *
 * <p>Never instantiated. The {@code Screen} super-interface is satisfied with stub no-op
 * implementations purely so the {@link com.codeheadsystems.game.di.ScreenKey} annotation
 * (which requires {@code Class<? extends Screen>}) accepts this class as a key.
 */
public final class SampleScreenMarker implements Screen {
    private SampleScreenMarker() {
        throw new UnsupportedOperationException("Marker class — not instantiable.");
    }

    @Override public void show() {}
    @Override public void render(float delta) {}
    @Override public void resize(int width, int height) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() {}
}
