package com.codeheadsystems.game.screens;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.codeheadsystems.game.flow.SessionResult;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * Generic end-of-session screen. Reads display text from {@link SessionResult} so the
 * screen has no compile-time dependency on demo state — the game populates the result
 * just before navigating here.
 */
@Singleton
public class GameOverScreen extends BaseScreen {

    private final SessionResult result;
    private final Label headlineLabel;
    private final Label detailLabel;
    private final TextButton tryAgain;

    @Inject
    public GameOverScreen(Skin skin, Provider<ScreenNavigator> nav, SessionResult result) {
        this.result = result;

        Table table = new Table();
        table.setFillParent(true);
        table.defaults().pad(6);

        headlineLabel = new Label("", skin);
        table.add(headlineLabel).padBottom(20).row();

        detailLabel = new Label("", skin);
        table.add(detailLabel).padBottom(20).row();

        tryAgain = new TextButton("Try Again", skin);
        tryAgain.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                result.onRetry.run();
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
        headlineLabel.setText(result.headline);
        detailLabel.setText(result.detail);
        tryAgain.setDisabled(!result.retryAvailable);
        tryAgain.setVisible(result.retryAvailable);
    }
}
