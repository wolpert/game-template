package com.codeheadsystems.game.screens;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * Stub level picker — exposes the empty scaffold {@link GameScreen} unconditionally and offers
 * the dodge demo via {@link ScreenNavigator#goToSampleGame()} only when it's wired into the
 * Dagger graph (i.e. {@code SampleModule} is in {@code GameComponent.modules}). The "Dodge
 * Sample" button hides cleanly when the sample is removed, so this screen never imports the
 * {@code sample/} package.
 */
@Singleton
public class LevelPickerScreen extends BaseScreen {

    private final Provider<ScreenNavigator> navProvider;

    @Inject
    public LevelPickerScreen(SpriteBatch batch, Skin skin, Provider<ScreenNavigator> navProvider) {
        super(batch);
        this.navProvider = navProvider;
        // navProvider returns the @Singleton ScreenNavigator — safe to resolve once at
        // construction since the navigator's own Provider<Screen> deps stay lazy.
        ScreenNavigator nav = navProvider.get();

        Table table = new Table();
        table.setFillParent(true);
        table.defaults().pad(6).width(220).height(48);

        table.add(new Label("Choose Level", skin)).padBottom(20).row();

        TextButton emptyGame = new TextButton("Empty Game", skin);
        emptyGame.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                nav.goToGame();
            }
        });
        table.add(emptyGame).row();

        if (nav.hasSampleGame()) {
            TextButton sample = new TextButton("Dodge Sample", skin);
            sample.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    nav.goToSampleGame();
                }
            });
            table.add(sample).row();
        }

        TextButton back = new TextButton("Back", skin);
        back.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                nav.goToMainMenu();
            }
        });
        table.add(back).width(150).height(40).padTop(20).row();

        stage.addActor(table);
    }

    @Override
    protected void onBack() {
        navProvider.get().goToMainMenu();
    }
}
