package com.codeheadsystems.game.config;

public class GameConfig {
    // Add new config sections here. Mirror the field name in assets/config/game.yaml.
    public String title;
    public LogoConfig logo;
    public PlayerConfig player;
    public PhysicsConfig physics;

    public static class LogoConfig {
        public float x;
        public float y;
    }

    public static class PlayerConfig {
        public float speed;
    }

    public static class PhysicsConfig {
        public Vec2Config gravity;
        public float pixelsPerMeter;
    }

    public static class Vec2Config {
        public float x;
        public float y;
    }
}
