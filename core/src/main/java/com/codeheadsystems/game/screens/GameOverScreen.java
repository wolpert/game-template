package com.codeheadsystems.game.screens;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.codeheadsystems.game.session.GameState;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
public class GameOverScreen extends BaseScreen {

    private final GameState state;
    private final Label scoreLabel;

    @Inject
    public GameOverScreen(Skin skin, Provider<ScreenNavigator> nav, GameState state) {
        this.state = state;

        Table table = new Table();
        table.setFillParent(true);
        table.defaults().pad(6);

        table.add(new Label("Game Over", skin)).padBottom(20).row();

        scoreLabel = new Label("", skin);
        table.add(scoreLabel).padBottom(20).row();

        TextButton tryAgain = new TextButton("Try Again", skin);
        tryAgain.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                nav.get().goToGame();
            }
        });
        table.add(tryAgain).width(200).height(48).row();

        TextButton menu = new TextButton("Main Menu", skin);
        menu.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                nav.get().goToMainMenu();
            }
        });
        table.add(menu).width(200).height(48).row();

        stage.addActor(table);
    }

    @Override
    public void show() {
        super.show();
        scoreLabel.setText(String.format("Time survived: %.1f s", state.elapsedSec));
    }
}
