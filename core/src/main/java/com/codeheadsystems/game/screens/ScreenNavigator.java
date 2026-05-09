package com.codeheadsystems.game.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * Single source of truth for screen transitions. Screens register themselves into a Dagger
 * {@code Map<Class<? extends Screen>, Provider<Screen>>} via
 * {@code @Provides @IntoMap @ScreenKey(SomeScreen.class)} in the providing module, and the
 * navigator dispatches by class through {@link #goTo(Class)}. Adding a new screen is now a
 * <b>single edit</b> (the {@code @IntoMap} provider in the owning module) — no matching field,
 * ctor param, {@code goToXxx()} method, or {@code disposeAll()} guard line are required.
 *
 * <p>Each entry is held as a {@link Provider} so its Scene2D tree is only built when first
 * navigated to — important for cold-start on mobile where {@link LoadingScreen} should reach the
 * GPU before any sibling screen does any work. {@link #disposeAll()} only resolves screens that
 * were actually visited; resolving a provider that was never used would force its Scene2D tree
 * to be built just to throw it away — and for {@link GameScreen} would also resolve atlas/texture
 * providers that may not yet be valid.
 *
 * <p>Screens themselves take a {@code Provider<ScreenNavigator>} to break the construction cycle
 * (the navigator transitively references every screen).
 *
 * <p><b>Optional sample-screen seam.</b> The dodge demo's {@code SampleGameScreen} contributes a
 * map entry keyed on {@link SampleScreenMarker}; the scaffold references that marker class
 * without importing {@code sample/}. When {@code SampleModule} is removed from
 * {@code GameComponent.modules}, the map simply has no entry for that key — {@link
 * #has(Class)} returns false, {@link #goTo(Class)} becomes a no-op, and {@code LevelPickerScreen}
 * hides its "Dodge Sample" button automatically.
 *
 * <p>Convenience wrappers ({@link #goToLoading()}, {@link #goToMainMenu()}, etc.) remain as
 * one-liners delegating to {@link #goTo(Class)} so existing callers stay unchanged. The
 * {@link #hasSampleGame()} / {@link #goToSampleGame()} pair is preserved as thin wrappers around
 * {@link SampleScreenMarker}.
 */
@Singleton
public class ScreenNavigator {

    private final Game game;
    private final Map<Class<? extends Screen>, Provider<Screen>> screens;
    private final Set<Class<? extends Screen>> resolved = new HashSet<>();

    @Inject
    public ScreenNavigator(Game game,
                           Map<Class<? extends Screen>, Provider<Screen>> screens) {
        this.game = game;
        this.screens = screens;
    }

    /** True iff {@code key} has been registered into the screen map by some module. */
    public boolean has(Class<? extends Screen> key) {
        return screens.containsKey(key);
    }

    /**
     * Navigate to the screen registered under {@code key}. No-op when no module contributes an
     * entry for {@code key} — this is the seam the optional sample screen relies on.
     */
    public void goTo(Class<? extends Screen> key) {
        Provider<Screen> provider = screens.get(key);
        if (provider == null) {
            return;
        }
        resolved.add(key);
        game.setScreen(provider.get());
    }

    public void goToLoading() { goTo(LoadingScreen.class); }
    public void goToMainMenu() { goTo(MainMenuScreen.class); }
    public void goToPreferences() { goTo(PreferencesScreen.class); }
    public void goToLevelPicker() { goTo(LevelPickerScreen.class); }
    public void goToGame() { goTo(GameScreen.class); }
    public void goToGameOver() { goTo(GameOverScreen.class); }

    /** True when {@code SampleModule} is wired into {@code GameComponent} (the demo is shipped). */
    public boolean hasSampleGame() {
        return has(SampleScreenMarker.class);
    }

    /**
     * Navigate to the demo's sample game screen. No-op when the sample isn't on the classpath
     * (i.e. {@code SampleModule.class} has been removed from {@code GameComponent.modules}).
     */
    public void goToSampleGame() {
        goTo(SampleScreenMarker.class);
    }

    /**
     * Dispose every screen actually constructed during this run. See class doc for why we skip
     * unresolved providers.
     */
    public void disposeAll() {
        for (Class<? extends Screen> key : resolved) {
            screens.get(key).get().dispose();
        }
    }
}
