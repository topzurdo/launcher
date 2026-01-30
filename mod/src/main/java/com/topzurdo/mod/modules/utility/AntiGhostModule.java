package com.topzurdo.mod.modules.utility;

import com.topzurdo.mod.modules.Module;
import com.topzurdo.mod.modules.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Anti Ghost Module - removes ghost blocks
 */
public class AntiGhostModule extends Module {

    private Setting<Integer> radius;

    public AntiGhostModule() {
        super("anti_ghost", "AntiGhost", "Убирает призрачные блоки", Category.UTILITY);

        radius = addSetting(Setting.ofInt("radius", "Радиус", "Радиус проверки", 4, 1, 8));
    }

    public void resync() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        int r = radius.getValue();
        BlockPos pos = mc.player.getBlockPos();

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos checkPos = pos.add(x, y, z);
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                        PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK,
                        checkPos,
                        Direction.UP
                    ));
                }
            }
        }
    }
}
