package com.codeheadsystems.game.screens;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * Initial screen. Currently a fixed-duration placeholder so the navigation flow is visible.
 * To make it real: wire in an AssetManager, queue assets here, and gate the transition on
 * {@code assets.update()} returning true (with a min-display time so flashes are avoided).
 */
@Singleton
public class LoadingScreen extends BaseScreen {

    private static final float MIN_DURATION = 0.8f;

    private final Provider<ScreenNavigator> nav;
    private float elapsed;

    @Inject
    public LoadingScreen(Skin skin, Provider<ScreenNavigator> nav) {
        this.nav = nav;
        Table table = new Table();
        table.setFillParent(true);
        table.add(new Label("Loading...", skin));
        stage.addActor(table);
    }

    @Override
    public void show() {
        super.show();
        elapsed = 0f;
    }

    @Override
    public void render(float delta) {
        super.render(delta);
        elapsed += delta;
        if (elapsed >= MIN_DURATION) {
            nav.get().goToMainMenu();
        }
    }
}
