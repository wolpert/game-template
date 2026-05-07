package com.codeheadsystems.game.di;

import com.badlogic.gdx.Game;
import com.codeheadsystems.game.TheGame;
import dagger.BindsInstance;
import dagger.Component;
import javax.inject.Singleton;

@Singleton
@Component(modules = GameModule.class)
public interface GameComponent {
    void inject(TheGame game);

    @Component.Builder
    interface Builder {
        @BindsInstance Builder game(Game game);
        GameComponent build();
    }
}
