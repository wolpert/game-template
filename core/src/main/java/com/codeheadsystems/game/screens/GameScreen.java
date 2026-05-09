package com.codeheadsystems.game.screens;

import com.badlogic.ashley.core.Engine;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.codeheadsystems.game.debug.DebugOverlay;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * Minimal scaffold game screen — an empty placeholder with a hint label and BACK/ESC → main menu
 * (routed through {@link BaseScreen#onBack()}). The dodge demo's playable screen lives at
 * {@code com.codeheadsystems.game.sample.SampleGameScreen}; this class is intentionally empty so a
 * new project starts from a clean slate. {@link #show()} tears down any prior session (entities +
 * Box2D bodies) so visiting the placeholder after the sample doesn't leak simulation state.
 */
@Singleton
public class GameScreen extends BaseScreen {

    private final Engine engine;
    private final World world;
    private final Provider<ScreenNavigator> nav;
    private final DebugOverlay debugOverlay;

    @Inject
    public GameScreen(Engine engine,
                      World world,
                      Provider<ScreenNavigator> nav,
                      DebugOverlay debugOverlay,
                      Skin skin) {
        this.engine = engine;
        this.world = world;
        this.nav = nav;
        this.debugOverlay = debugOverlay;

        Label hint = new Label("Empty game screen — press Esc to return", skin);
        Table table = new Table();
        table.setFillParent(true);
        table.center();
        table.add(hint);
        stage.addActor(table);
    }

    @Override
    public void show() {
        super.show();
        // Tear down anything a previous (sample) session left behind so the placeholder is clean.
        engine.removeAllEntities();
        Array<Body> bodies = new Array<>();
        world.getBodies(bodies);
        for (Body b : bodies) {
            world.destroyBody(b);
        }
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0.10f, 0.10f, 0.15f, 1f);
        engine.update(delta);
        stage.act(delta);
        stage.draw();
        debugOverlay.render(delta);
    }

    @Override
    protected void onBack() {
        nav.get().goToMainMenu();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        debugOverlay.resize(width, height);
    }

    @Override
    public void dispose() {
        super.dispose();
        debugOverlay.dispose();
    }
}
