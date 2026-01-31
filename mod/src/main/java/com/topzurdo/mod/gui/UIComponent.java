package com.topzurdo.mod.gui;

import net.minecraft.client.util.math.MatrixStack;

/**
 * Base interface for all UI components
 */
public interface UIComponent {
    void render(MatrixStack ms, int mouseX, int mouseY, float delta);
    
    default boolean mouseClicked(double mouseX, double mouseY, int button) { return false; }
    default boolean mouseReleased(double mouseX, double mouseY, int button) { return false; }
    default boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) { return false; }
    default boolean mouseScrolled(double mouseX, double mouseY, double amount) { return false; }
    default boolean keyPressed(int keyCode, int scanCode, int modifiers) { return false; }
    default boolean charTyped(char chr, int modifiers) { return false; }
    
    int getX();
    int getY();
    int getWidth();
    int getHeight();
    void setPosition(int x, int y);
    default boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= getX() && mouseX < getX() + getWidth() && mouseY >= getY() && mouseY < getY() + getHeight();
    }
    default void setPartialTicks(float pt) { }
}
