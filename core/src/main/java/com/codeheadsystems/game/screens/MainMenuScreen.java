package com.codeheadsystems.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
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

    private final Skin skin;
    private boolean quitDialogOpen;

    @Inject
    public MainMenuScreen(Skin skin, Provider<ScreenNavigator> nav) {
        this.skin = skin;
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

    /**
     * BACK/ESCAPE on the root menu shows a confirm-quit dialog. The Scene2D {@link Dialog} also
     * binds ESCAPE/BACK to "No" so a second BACK press dismisses the prompt; without that,
     * {@link BaseScreen}'s adapter would re-trigger {@link #onBack()} and the dialog would never
     * close. The {@code quitDialogOpen} flag is a defense-in-depth guard so rapid presses can't
     * stack dialogs even if a focus quirk slips an event past the dialog's key handlers.
     */
    @Override
    protected void onBack() {
        if (quitDialogOpen) {
            return;
        }
        quitDialogOpen = true;
        Dialog dialog = new Dialog("Quit?", skin) {
            @Override
            protected void result(Object object) {
                quitDialogOpen = false;
                if (Boolean.TRUE.equals(object)) {
                    Gdx.app.exit();
                }
            }
        };
        dialog.text("Are you sure you want to quit?");
        dialog.button("Yes", Boolean.TRUE);
        dialog.button("No", Boolean.FALSE);
        dialog.key(Input.Keys.ENTER, Boolean.TRUE);
        dialog.key(Input.Keys.ESCAPE, Boolean.FALSE);
        dialog.key(Input.Keys.BACK, Boolean.FALSE);
        dialog.show(stage);
    }
}
