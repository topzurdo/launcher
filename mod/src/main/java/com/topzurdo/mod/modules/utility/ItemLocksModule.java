package com.topzurdo.mod.modules.utility;

import com.topzurdo.mod.modules.Module;
import com.topzurdo.mod.modules.Setting;

import java.util.HashSet;
import java.util.Set;

/**
 * Item Locks Module - prevents dropping certain items
 */
public class ItemLocksModule extends Module {

    private Setting<Boolean> lockHotbar;
    private Setting<Boolean> lockArmor;
    private Setting<Boolean> lockOffhand;

    private final Set<Integer> lockedSlots = new HashSet<>();

    public ItemLocksModule() {
        super("item_locks", "Item Locks", "Блокировка выбрасывания предметов", Category.UTILITY);

        lockHotbar = addSetting(Setting.ofBoolean("lock_hotbar", "Блокировать хотбар", "Запретить выбрасывать из хотбара", false));
        lockArmor = addSetting(Setting.ofBoolean("lock_armor", "Блокировать броню", "Запретить снимать броню", false));
        lockOffhand = addSetting(Setting.ofBoolean("lock_offhand", "Блокировать оффхенд", "Запретить выбрасывать из левой руки", true));
    }

    public boolean isSlotLocked(int slot) {
        if (!isEnabled()) return false;
        if (lockHotbar.getValue() && slot >= 0 && slot < 9) return true;
        if (lockArmor.getValue() && slot >= 36 && slot <= 39) return true;
        if (lockOffhand.getValue() && slot == 40) return true;
        return lockedSlots.contains(slot);
    }

    public void toggleLock(int slot) {
        if (lockedSlots.contains(slot)) lockedSlots.remove(slot);
        else lockedSlots.add(slot);
    }

    public void toggleSlotLock(int slot) {
        toggleLock(slot);
    }

    public boolean shouldHandleMouseClick(int button) {
        return isEnabled() && button == 0 && org.lwjgl.glfw.GLFW.glfwGetKey(
            net.minecraft.client.MinecraftClient.getInstance().getWindow().getHandle(),
            org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT
        ) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
    }

    public boolean shouldPreventMove(int slot) {
        return isEnabled() && isSlotLocked(slot);
    }

    public boolean shouldPreventDrop(int slot) {
        return isEnabled() && isSlotLocked(slot);
    }
}
