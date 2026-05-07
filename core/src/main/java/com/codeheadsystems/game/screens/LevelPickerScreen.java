package com.codeheadsystems.game.screens;

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
 * Stub level picker — single hardcoded level. Replace with a list backed by config or
 * a directory scan when there are real levels to pick from.
 */
@Singleton
public class LevelPickerScreen extends BaseScreen {

    @Inject
    public LevelPickerScreen(Skin skin, Provider<ScreenNavigator> nav) {
        Table table = new Table();
        table.setFillParent(true);
        table.defaults().pad(6).width(220).height(48);

        table.add(new Label("Choose Level", skin)).padBottom(20).row();

        TextButton level1 = new TextButton("Level 1", skin);
        level1.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                nav.get().goToGame();
            }
        });
        table.add(level1).row();

        TextButton back = new TextButton("Back", skin);
        back.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                nav.get().goToMainMenu();
            }
        });
        table.add(back).width(150).height(40).padTop(20).row();

        stage.addActor(table);
    }
}
