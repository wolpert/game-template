package com.codeheadsystems.game.screens;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.codeheadsystems.game.di.GameModule;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * Drives initial asset loading via {@link AssetManager} and reflects progress with a Scene2D
 * {@link ProgressBar}. The transition out is gated on both the manager finishing AND a minimum
 * display time — instant loads otherwise flash by unreadably.
 *
 * <p>The {@link Skin} itself is loaded eagerly by Dagger (not via the manager) since this screen
 * needs it to render the bar — the chicken-and-egg is unavoidable for the screen that displays
 * loading progress for everything else.
 */
@Singleton
public class LoadingScreen extends BaseScreen {

    private static final float MIN_DURATION = 0.8f;

    private final Provider<ScreenNavigator> nav;
    private final AssetManager assets;
    private final ProgressBar progressBar;
    private final Label percentLabel;
    private float elapsed;
    private boolean queued;

    @Inject
    public LoadingScreen(Skin skin, Provider<ScreenNavigator> nav, AssetManager assets) {
        this.nav = nav;
        this.assets = assets;

        progressBar = new ProgressBar(0f, 1f, 0.01f, false, skin);
        progressBar.setAnimateDuration(0.1f);
        percentLabel = new Label("0%", skin);

        Table table = new Table();
        table.setFillParent(true);
        table.add(new Label("Loading...", skin)).padBottom(16).row();
        table.add(progressBar).width(360f).padBottom(8).row();
        table.add(percentLabel);
        stage.addActor(table);
    }

    @Override
    public void show() {
        super.show();
        elapsed = 0f;
        if (!queued) {
            assets.load(GameModule.LOGO_TEXTURE_PATH, Texture.class);
            assets.load(GameModule.GAME_ATLAS_PATH, TextureAtlas.class);
            queued = true;
        }
    }

    @Override
    public void render(float delta) {
        boolean done = assets.update();
        float progress = assets.getProgress();
        progressBar.setValue(progress);
        percentLabel.setText(((int) (progress * 100f)) + "%");

        super.render(delta);
        elapsed += delta;
        if (done && elapsed >= MIN_DURATION) {
            nav.get().goToMainMenu();
        }
    }
}
