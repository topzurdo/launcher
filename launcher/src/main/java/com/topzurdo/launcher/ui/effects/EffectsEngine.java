package com.topzurdo.launcher.ui.effects;

import javafx.scene.layout.Pane;

/**
 * Particle / visual effects (no-op implementation).
 */
public class EffectsEngine {

    public void createParticleEffect(String id, Pane root) {
        // no-op
    }

    public void triggerBurst(String id, double x, double y, int count) {
        // no-op
    }

    public void enableEffects() { }
    public void disableEffects() { }
    public void shutdown() { }
}
