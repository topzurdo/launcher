package com.topzurdo.mod.gui;

import net.minecraft.client.util.math.MatrixStack;

/**
 * Base interface for all UI components
 */
public interface UIComponent {
    void render(MatrixStack ms, int mouseX, int mouseY, float delta);
    boolean mouseClicked(double mouseX, double mouseY, int button);
    boolean mouseReleased(double mouseX, double mouseY, int button);
    boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY);
    boolean mouseScrolled(double mouseX, double mouseY, double amount);
    boolean keyPressed(int keyCode, int scanCode, int modifiers);
    boolean charTyped(char chr, int modifiers);
    
    int getX();
    int getY();
    int getWidth();
    int getHeight();
    void setPosition(int x, int y);
    boolean isMouseOver(double mouseX, double mouseY);
}
