package com.topzurdo.mod;

/**
 * Constants used throughout the mod
 */
public final class ModConstants {

    private ModConstants() {}

    public static final String MOD_ID = "topzurdo";
    public static final String MOD_NAME = "TopZurdo Client";
    public static final String VERSION = "1.0.0";

    public static final class Config {
        public static final String CONFIG_DIR = "topzurdo";
        public static final String CONFIG_FILE = "modules.json";
    }

    public static final class UI {
        public static final int ANIMATION_DURATION_MS = 200;
        public static final float ANIMATION_SPEED = 0.15f;
        /** Путь к превью модулей: assets/topzurdo/previews/<moduleId>.png (напр. fullbright.png). */
        public static final String PREVIEWS_PATH = "textures/previews/";
    }

    public static final class Performance {
        public static final double SMALL_ENTITY_DISTANCE = 16.0;
        public static final double ITEM_FRAME_PAINTING_CULL_DISTANCE = 32.0;
        public static final double BEHIND_CULL_THRESHOLD = -0.5;
        public static final double BEHIND_CULL_MIN_DISTANCE = 8.0;
        /** Max particles per tick when FPS Boost limits particles. */
        public static final int FPS_BOOST_MAX_PARTICLES = 1000;
        /** Particle cull distance (blocks). */
        public static final int FPS_BOOST_PARTICLE_CULL_DISTANCE = 32;
        /** Entity culling: max distance (blocks). */
        public static final int FPS_BOOST_ENTITY_CULL_DISTANCE = 64;
        /** Dynamic FPS: target FPS when window is in background. */
        public static final int FPS_BOOST_BACKGROUND_FPS = 20;
        /** Default max FPS when focused (0 = vsync/unlimited). */
        public static final int FPS_BOOST_DEFAULT_MAX_FPS = 0;
    }
}
