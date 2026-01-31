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
    /** Width of content zone (excluding gutter); if > 0, scrollbar is drawn in gutter at x + contentZoneWidth + 2. */
    private int contentZoneWidth = -1;
    private int contentHeight;
    private float scrollOffset = 0;
    private float targetScroll = 0;
    private float scrollVelocity = 0f;
    private boolean dragging = false;

    public ScrollContainer(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.contentHeight = height;
    }

    public ScrollContainer(int x, int y, int width, int height, int contentHeight) {
        this(x, y, width, height);
        this.contentHeight = contentHeight;
    }

    public void setContentHeight(int contentHeight) {
        this.contentHeight = contentHeight;
    }

    /** Set width of content zone; scrollbar will be drawn in gutter at x + contentZoneWidth + 2. */
    public void setContentZoneWidth(int contentZoneWidth) {
        this.contentZoneWidth = contentZoneWidth;
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
        // Momentum: apply velocity
        targetScroll += scrollVelocity;
        scrollVelocity *= 0.92f;
        if (Math.abs(scrollVelocity) < 0.1f) scrollVelocity = 0f;
        clampScroll();

        // Smooth scrolling (delta-based for frame-rate independence)
        scrollOffset += (targetScroll - scrollOffset) * (1f - (float) Math.pow(0.7f, Math.min(delta * 60f, 2f)));
        renderScrollbar(ms, mouseX, mouseY);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height) {
            scrollVelocity += amount * 0.5f;
            clampScroll();
            return true;
        }
        return false;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Check if clicking on scrollbar
        int barLeft = contentZoneWidth > 0 ? x + contentZoneWidth : x + width - 8;
        if (button == 0 && mouseX >= barLeft && mouseX < x + width && mouseY >= y && mouseY < y + height) {
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

    public int getScrollOffset() {
        scrollOffset += (targetScroll - scrollOffset) * 0.35f;
        return Math.round(scrollOffset);
    }
    public void setScrollOffset(float offset) { this.scrollOffset = offset; this.targetScroll = offset; }
    public void setScrollOffset(int offset) { setScrollOffset((float) offset); }
    public void setPosition(int x, int y) { this.x = x; this.y = y; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public int getMaxScroll() {
        return Math.max(0, contentHeight - height);
    }

    /** Positive amount = scroll down (content moves up). Adds to velocity for momentum. */
    public void addScroll(double amount) {
        scrollVelocity += (float) amount * 0.5f;
        clampScroll();
    }

    private static final int TRACK_RADIUS = 2;
    private static final int MIN_THUMB_HEIGHT = 24;

    public void renderScrollbar(net.minecraft.client.util.math.MatrixStack ms) {
        renderScrollbar(ms, -999, -999);
    }

    public void renderScrollbar(net.minecraft.client.util.math.MatrixStack ms, int mouseX, int mouseY) {
        if (contentHeight <= height) return;
        int barW = OceanTheme.SCROLLBAR_WIDTH;
        int thumbRadius = OceanTheme.SCROLLBAR_THUMB_RADIUS;
        int scrollbarX = contentZoneWidth > 0 ? x + contentZoneWidth + 2 : x + width - barW - 2;
        int scrollbarHeight = Math.max(MIN_THUMB_HEIGHT, (int) ((float) height / contentHeight * height));
        int maxScrollRange = height - scrollbarHeight;
        int scrollbarY = maxScrollRange <= 0 ? y : y + (int) ((getScrollOffset() / (float) getMaxScroll()) * maxScrollRange);
        scrollbarY = Math.max(y, Math.min(y + height - scrollbarHeight, scrollbarY));

        // Дорожка (track) — видимая область скролла
        int trackColor = UIRenderHelper.withAlpha(OceanTheme.BG_ELEVATED, 0.9f);
        UIRenderHelper.fillRoundRect(ms, scrollbarX, y, barW, height, TRACK_RADIUS, trackColor);

        // Thumb (rounded, hover highlight)
        boolean overThumb = isOverThumb(mouseX, mouseY);
        int thumbColor = OceanTheme.ACCENT;
        if (overThumb) {
            int r = Math.min(255, ((OceanTheme.ACCENT >> 16) & 0xFF) + 25);
            int g = Math.min(255, ((OceanTheme.ACCENT >> 8) & 0xFF) + 25);
            int b = Math.min(255, (OceanTheme.ACCENT & 0xFF) + 25);
            thumbColor = (OceanTheme.ACCENT & 0xFF000000) | (r << 16) | (g << 8) | b;
        }
        UIRenderHelper.fillRoundRect(ms, scrollbarX, scrollbarY, barW, scrollbarHeight, thumbRadius, thumbColor);
    }

    public boolean isOverThumb(int mouseX, int mouseY) {
        if (contentHeight <= height) return false;
        int barW = OceanTheme.SCROLLBAR_WIDTH;
        int scrollbarX = contentZoneWidth > 0 ? x + contentZoneWidth + 2 : x + width - barW - 2;
        int scrollbarHeight = Math.max(MIN_THUMB_HEIGHT, (int) ((float) height / contentHeight * height));
        int maxScrollRange = height - scrollbarHeight;
        int scrollbarY = maxScrollRange <= 0 ? y : y + (int) ((getScrollOffset() / (float) getMaxScroll()) * maxScrollRange);
        scrollbarY = Math.max(y, Math.min(y + height - scrollbarHeight, scrollbarY));
        return mouseX >= scrollbarX && mouseX < scrollbarX + barW && mouseY >= scrollbarY && mouseY < scrollbarY + scrollbarHeight;
    }

    public boolean isOverScrollbar(int mouseX, int mouseY) {
        int barLeft = contentZoneWidth > 0 ? x + contentZoneWidth : x + width - OceanTheme.SCROLLBAR_WIDTH - 4;
        return mouseX >= barLeft && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public void setOffsetFromScrollbarMouseY(int mouseY) {
        if (contentHeight <= height) return;
        int scrollbarHeight = Math.max(MIN_THUMB_HEIGHT, (int) ((float) height / contentHeight * height));
        int maxScrollRange = height - scrollbarHeight;
        float ratio = (float)(mouseY - y - scrollbarHeight / 2) / maxScrollRange;
        ratio = Math.max(0, Math.min(1, ratio));
        targetScroll = ratio * getMaxScroll();
        clampScroll();
    }
}
