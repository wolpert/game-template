package com.codeheadsystems.game.config;

public class GameConfig {
    public String title;
    public LogoConfig logo;
    public PlayerConfig player;

    public static class LogoConfig {
        public float x;
        public float y;
    }

    public static class PlayerConfig {
        public float speed;
    }
}
