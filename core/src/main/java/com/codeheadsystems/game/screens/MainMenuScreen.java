package com.codeheadsystems.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
public class MainMenuScreen extends BaseScreen {

    @Inject
    public MainMenuScreen(Skin skin, Provider<ScreenNavigator> nav) {
        Table table = new Table();
        table.setFillParent(true);
        table.defaults().pad(6).width(220).height(48);

        table.add(new Label("Game Template", skin)).padBottom(20).row();

        TextButton play = new TextButton("Play", skin);
        play.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                nav.get().goToLevelPicker();
            }
        });
        table.add(play).row();

        TextButton prefs = new TextButton("Preferences", skin);
        prefs.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                nav.get().goToPreferences();
            }
        });
        table.add(prefs).row();

        TextButton quit = new TextButton("Quit", skin);
        quit.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });
        table.add(quit).row();

        stage.addActor(table);
    }
}
