package com.codeheadsystems.game.di;

import com.codeheadsystems.game.TheGame;
import dagger.Component;
import javax.inject.Singleton;

@Singleton
@Component(modules = GameModule.class)
public interface GameComponent {
    void inject(TheGame game);
}
