package com.codeheadsystems.game.screens;

import com.badlogic.gdx.Game;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Single source of truth for screen transitions. Screens take a {@code Provider<ScreenNavigator>}
 * (lazy resolution breaks the construction cycle since the navigator depends on every screen);
 * the navigator depends on screens directly so it can {@link #disposeAll()} them at app shutdown.
 */
@Singleton
public class ScreenNavigator {

    private final Game game;
    private final LoadingScreen loadingScreen;
    private final MainMenuScreen mainMenuScreen;
    private final PreferencesScreen preferencesScreen;
    private final LevelPickerScreen levelPickerScreen;
    private final GameScreen gameScreen;

    @Inject
    public ScreenNavigator(Game game,
                           LoadingScreen loadingScreen,
                           MainMenuScreen mainMenuScreen,
                           PreferencesScreen preferencesScreen,
                           LevelPickerScreen levelPickerScreen,
                           GameScreen gameScreen) {
        this.game = game;
        this.loadingScreen = loadingScreen;
        this.mainMenuScreen = mainMenuScreen;
        this.preferencesScreen = preferencesScreen;
        this.levelPickerScreen = levelPickerScreen;
        this.gameScreen = gameScreen;
    }

    public void goToLoading() { game.setScreen(loadingScreen); }
    public void goToMainMenu() { game.setScreen(mainMenuScreen); }
    public void goToPreferences() { game.setScreen(preferencesScreen); }
    public void goToLevelPicker() { game.setScreen(levelPickerScreen); }
    public void goToGame() { game.setScreen(gameScreen); }

    public void disposeAll() {
        loadingScreen.dispose();
        mainMenuScreen.dispose();
        preferencesScreen.dispose();
        levelPickerScreen.dispose();
        gameScreen.dispose();
    }
}
