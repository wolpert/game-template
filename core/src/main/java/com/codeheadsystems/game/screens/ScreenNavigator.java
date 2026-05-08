package com.codeheadsystems.game.screens;

import com.badlogic.gdx.Game;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * Single source of truth for screen transitions. Screens take a {@code Provider<ScreenNavigator>}
 * (lazy resolution breaks the construction cycle since the navigator depends on every screen);
 * the navigator depends on screens directly so it can {@link #disposeAll()} them at app shutdown.
 *
 * <p>{@link GameScreen} is taken as a {@code Provider} because its transitive deps
 * ({@link com.badlogic.gdx.graphics.g2d.TextureAtlas}, {@link com.badlogic.gdx.graphics.Texture})
 * are sourced from the {@link com.badlogic.gdx.assets.AssetManager} and are only loaded once
 * {@link LoadingScreen} has run. Building it eagerly at app start would resolve those providers
 * before the manager has been queued.
 */
@Singleton
public class ScreenNavigator {

    private final Game game;
    private final LoadingScreen loadingScreen;
    private final MainMenuScreen mainMenuScreen;
    private final PreferencesScreen preferencesScreen;
    private final LevelPickerScreen levelPickerScreen;
    private final Provider<GameScreen> gameScreenProvider;
    private final GameOverScreen gameOverScreen;

    private boolean gameScreenBuilt;

    @Inject
    public ScreenNavigator(Game game,
                           LoadingScreen loadingScreen,
                           MainMenuScreen mainMenuScreen,
                           PreferencesScreen preferencesScreen,
                           LevelPickerScreen levelPickerScreen,
                           Provider<GameScreen> gameScreenProvider,
                           GameOverScreen gameOverScreen) {
        this.game = game;
        this.loadingScreen = loadingScreen;
        this.mainMenuScreen = mainMenuScreen;
        this.preferencesScreen = preferencesScreen;
        this.levelPickerScreen = levelPickerScreen;
        this.gameScreenProvider = gameScreenProvider;
        this.gameOverScreen = gameOverScreen;
    }

    public void goToLoading() { game.setScreen(loadingScreen); }
    public void goToMainMenu() { game.setScreen(mainMenuScreen); }
    public void goToPreferences() { game.setScreen(preferencesScreen); }
    public void goToLevelPicker() { game.setScreen(levelPickerScreen); }
    public void goToGame() {
        gameScreenBuilt = true;
        game.setScreen(gameScreenProvider.get());
    }
    public void goToGameOver() { game.setScreen(gameOverScreen); }

    public void disposeAll() {
        loadingScreen.dispose();
        mainMenuScreen.dispose();
        preferencesScreen.dispose();
        levelPickerScreen.dispose();
        // Only dispose GameScreen if we ever built it; otherwise calling provider.get() would
        // construct it (and force atlas/texture resolution) just to throw the result away.
        if (gameScreenBuilt) {
            gameScreenProvider.get().dispose();
        }
        gameOverScreen.dispose();
    }
}
