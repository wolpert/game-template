package com.codeheadsystems.game.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.StringReader;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.error.YAMLException;

class ConfigLoaderTest {

    private final ConfigLoader loader = new ConfigLoader();

    @Test
    void parsesNestedYamlIntoPublicFields() {
        String yaml = """
                title: My Game
                logo:
                  x: 12.5
                  y: 34
                """;

        GameConfig config = loader.load(GameConfig.class, new StringReader(yaml));

        assertEquals("My Game", config.title);
        assertEquals(12.5f, config.logo.x);
        assertEquals(34f, config.logo.y);
    }

    @Test
    void rejectsUnknownFields() {
        // SnakeYAML's strict POJO constructor catches typos in config keys instead of silently ignoring them.
        String yaml = """
                title: Oops
                logoo:
                  x: 1
                  y: 2
                """;

        assertThrows(YAMLException.class, () -> loader.load(GameConfig.class, new StringReader(yaml)));
    }
}
