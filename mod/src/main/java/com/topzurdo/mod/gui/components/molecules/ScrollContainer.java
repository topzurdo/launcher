package com.topzurdo.mod.gui.components.molecules;

import com.topzurdo.mod.gui.OceanTheme;
import com.topzurdo.mod.gui.UIRenderHelper;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.opengl.GL11;

/**
 * Scrollable container component
 */
public class ScrollContainer {

    private int x, y, width, height;
    private int contentHeight;
    private float scrollOffset = 0;
    private float targetScroll = 0;
    private boolean dragging = false;

    public ScrollContainer(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.contentHeight = height;
    }

    public void setContentHeight(int contentHeight) {
        this.contentHeight = contentHeight;
    }

    public void beginScissor() {
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        double scale = net.minecraft.client.MinecraftClient.getInstance().getWindow().getScaleFactor();
        int windowHeight = net.minecraft.client.MinecraftClient.getInstance().getWindow().getHeight();
        GL11.glScissor(
            (int) (x * scale),
            (int) ((windowHeight - (y + height) * scale)),
            (int) (width * scale),
            (int) (height * scale)
        );
    }

    public void endScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    public void render(MatrixStack ms, int mouseX, int mouseY, float delta) {
        // Smooth scrolling
        scrollOffset += (targetScroll - scrollOffset) * 0.3f;

        // Render scrollbar if needed
        if (contentHeight > height) {
            int scrollbarHeight = Math.max(20, (int) ((float) height / contentHeight * height));
            int scrollbarY = y + (int) ((scrollOffset / (contentHeight - height)) * (height - scrollbarHeight));

            // Track
            UIRenderHelper.drawRect(ms, x + width - 6, y, 4, height, OceanTheme.BG_ELEVATED);

            // Thumb
            UIRenderHelper.drawRoundRect(ms, x + width - 6, scrollbarY, 4, scrollbarHeight, 2, OceanTheme.ACCENT);
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height) {
            targetScroll -= amount * 20;
            clampScroll();
            return true;
        }
        return false;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Check if clicking on scrollbar
        if (button == 0 && mouseX >= x + width - 8 && mouseX < x + width && mouseY >= y && mouseY < y + height) {
            dragging = true;
            return true;
        }
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && dragging) {
            dragging = false;
            return true;
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging && contentHeight > height) {
            float scrollRatio = (float) (mouseY - y) / height;
            targetScroll = scrollRatio * (contentHeight - height);
            clampScroll();
            return true;
        }
        return false;
    }

    private void clampScroll() {
        int maxScroll = Math.max(0, contentHeight - height);
        targetScroll = Math.max(0, Math.min(maxScroll, targetScroll));
    }

    public float getScrollOffset() { return scrollOffset; }
    public void setScrollOffset(float offset) { this.scrollOffset = offset; this.targetScroll = offset; }
    public void setPosition(int x, int y) { this.x = x; this.y = y; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}
