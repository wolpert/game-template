package com.codeheadsystems.game.screens;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
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
    private final Provider<ScreenNavigator> nav;
    private final Label headlineLabel;
    private final Label detailLabel;
    private final Label bestLabel;
    private final Label newRecordLabel;
    private final TextButton tryAgain;

    @Inject
    public GameOverScreen(SpriteBatch batch, Skin skin, Provider<ScreenNavigator> nav, SessionResult result) {
        super(batch);
        this.result = result;
        this.nav = nav;

        Table table = new Table();
        table.setFillParent(true);
        table.defaults().pad(6);

        headlineLabel = new Label("", skin);
        table.add(headlineLabel).padBottom(20).row();

        detailLabel = new Label("", skin);
        table.add(detailLabel).padBottom(20).row();

        // bestSec is shown when populated (>0) — the empty scaffold GameScreen doesn't fill it,
        // so a session that didn't track a record naturally hides this row.
        bestLabel = new Label("", skin);
        table.add(bestLabel).padBottom(6).row();

        newRecordLabel = new Label("", skin);
        table.add(newRecordLabel).padBottom(20).row();

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
        if (result.bestSec > 0f) {
            bestLabel.setText(String.format("Best: %.1fs", result.bestSec));
            bestLabel.setVisible(true);
        } else {
            bestLabel.setText("");
            bestLabel.setVisible(false);
        }
        if (result.newRecord) {
            newRecordLabel.setText("+ NEW RECORD");
            newRecordLabel.setVisible(true);
        } else {
            newRecordLabel.setText("");
            newRecordLabel.setVisible(false);
        }
        tryAgain.setDisabled(!result.retryAvailable);
        tryAgain.setVisible(result.retryAvailable);
    }

    @Override
    protected void onBack() {
        nav.get().goToMainMenu();
    }
}
