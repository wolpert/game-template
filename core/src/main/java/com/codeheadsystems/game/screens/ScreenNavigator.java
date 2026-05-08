package com.codeheadsystems.game.screens;

import com.badlogic.gdx.Game;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * Single source of truth for screen transitions. Every screen is held as a {@link Provider}
 * so its Scene2D tree is only built when first navigated to — important for cold-start on
 * mobile where {@link LoadingScreen} should reach the GPU before any sibling screen does any
 * work. Screens themselves take a {@code Provider<ScreenNavigator>} to break the construction
 * cycle (the navigator references every screen).
 *
 * <p>{@link GameScreen}'s transitive deps ({@link com.badlogic.gdx.graphics.g2d.TextureAtlas},
 * {@link com.badlogic.gdx.graphics.Texture}) are sourced from the
 * {@link com.badlogic.gdx.assets.AssetManager} and aren't valid until {@link LoadingScreen} has
 * drained the manager — the lazy resolution that protects cold-start also protects that
 * ordering for free.
 */
@Singleton
public class ScreenNavigator {

    private final Game game;
    private final Provider<LoadingScreen> loadingScreen;
    private final Provider<MainMenuScreen> mainMenuScreen;
    private final Provider<PreferencesScreen> preferencesScreen;
    private final Provider<LevelPickerScreen> levelPickerScreen;
    private final Provider<GameScreen> gameScreen;
    private final Provider<GameOverScreen> gameOverScreen;

    private boolean loadingBuilt;
    private boolean mainMenuBuilt;
    private boolean preferencesBuilt;
    private boolean levelPickerBuilt;
    private boolean gameBuilt;
    private boolean gameOverBuilt;

    @Inject
    public ScreenNavigator(Game game,
                           Provider<LoadingScreen> loadingScreen,
                           Provider<MainMenuScreen> mainMenuScreen,
                           Provider<PreferencesScreen> preferencesScreen,
                           Provider<LevelPickerScreen> levelPickerScreen,
                           Provider<GameScreen> gameScreen,
                           Provider<GameOverScreen> gameOverScreen) {
        this.game = game;
        this.loadingScreen = loadingScreen;
        this.mainMenuScreen = mainMenuScreen;
        this.preferencesScreen = preferencesScreen;
        this.levelPickerScreen = levelPickerScreen;
        this.gameScreen = gameScreen;
        this.gameOverScreen = gameOverScreen;
    }

    public void goToLoading() { loadingBuilt = true; game.setScreen(loadingScreen.get()); }
    public void goToMainMenu() { mainMenuBuilt = true; game.setScreen(mainMenuScreen.get()); }
    public void goToPreferences() { preferencesBuilt = true; game.setScreen(preferencesScreen.get()); }
    public void goToLevelPicker() { levelPickerBuilt = true; game.setScreen(levelPickerScreen.get()); }
    public void goToGame() { gameBuilt = true; game.setScreen(gameScreen.get()); }
    public void goToGameOver() { gameOverBuilt = true; game.setScreen(gameOverScreen.get()); }

    /**
     * Dispose every screen actually constructed during this run. Resolving a provider that was
     * never visited would force its Scene2D tree to be built just to throw it away — and for
     * {@link GameScreen} that would also resolve atlas/texture providers that may not yet be
     * valid.
     */
    public void disposeAll() {
        if (loadingBuilt) loadingScreen.get().dispose();
        if (mainMenuBuilt) mainMenuScreen.get().dispose();
        if (preferencesBuilt) preferencesScreen.get().dispose();
        if (levelPickerBuilt) levelPickerScreen.get().dispose();
        if (gameBuilt) gameScreen.get().dispose();
        if (gameOverBuilt) gameOverScreen.get().dispose();
    }
}
