package com.codeheadsystems.game.screens;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * Stub preferences screen. The toggles are not persisted yet — wire in libGDX's
 * {@code Preferences} API or your YAML config writer when you need persistence.
 */
@Singleton
public class PreferencesScreen extends BaseScreen {

    @Inject
    public PreferencesScreen(Skin skin, Provider<ScreenNavigator> nav) {
        Table table = new Table();
        table.setFillParent(true);
        table.defaults().pad(6);

        table.add(new Label("Preferences", skin)).padBottom(20).row();

        CheckBox sound = new CheckBox(" Sound", skin);
        sound.setChecked(true);
        table.add(sound).left().row();

        CheckBox music = new CheckBox(" Music", skin);
        music.setChecked(true);
        table.add(music).left().row();

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
