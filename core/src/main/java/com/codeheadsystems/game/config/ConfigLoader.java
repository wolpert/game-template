package com.codeheadsystems.game.config;

import java.io.Reader;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.BeanAccess;

@Singleton
public class ConfigLoader {

    @Inject
    public ConfigLoader() {}

    public <T> T load(Class<T> type, Reader reader) {
        Constructor constructor = new Constructor(type, new LoaderOptions());
        // Match values to public fields directly so config POJOs don't need getters/setters.
        constructor.getPropertyUtils().setBeanAccess(BeanAccess.FIELD);
        return new Yaml(constructor).load(reader);
    }
}
