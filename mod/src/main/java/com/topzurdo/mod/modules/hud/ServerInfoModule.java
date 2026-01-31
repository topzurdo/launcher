package com.topzurdo.mod.modules.hud;

import com.topzurdo.mod.modules.Module;
import com.topzurdo.mod.modules.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Server Info Module - displays server information
 */
public class ServerInfoModule extends Module {

    private Setting<Integer> posX;
    private Setting<Integer> posY;
    private Setting<Boolean> showPing;
    private Setting<Boolean> showPlayers;

    public ServerInfoModule() {
        super("server_info", "Server Info", "Информация о сервере", Category.HUD);

        posX = addSetting(Setting.ofInt("pos_x", "Позиция X", "Горизонтальная позиция", 10, 0, 500));
        posY = addSetting(Setting.ofInt("pos_y", "Позиция Y", "Вертикальная позиция", 50, 0, 500));
        showPing = addSetting(Setting.ofBoolean("show_ping", "Пинг", "Показывать пинг", true));
        showPlayers = addSetting(Setting.ofBoolean("show_players", "Игроки", "Показывать кол-во игроков", true));
    }

    @Override
    public void onRender(float partialTicks) {
        if (!isEnabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        int x = posX.getValue();
        int y = posY.getValue();
        MatrixStack ms = new MatrixStack();
        TextRenderer tr = mc.textRenderer;

        ServerInfo info = mc.getCurrentServerEntry();
        if (info != null) {
            tr.draw(ms, "Server: " + info.address, x, y, 0xFFFFFF);
            y += 10;

            if (showPing.getValue() && mc.getNetworkHandler() != null) {
                PlayerListEntry playerInfo = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
                if (playerInfo != null) {
                    int ping = playerInfo.getLatency();
                    int color = ping < 50 ? 0x55FF55 : ping < 100 ? 0xFFFF55 : 0xFF5555;
                    tr.draw(ms, "Ping: " + ping + "ms", x, y, color);
                    y += 10;
                }
            }

            if (showPlayers.getValue() && mc.getNetworkHandler() != null) {
                int players = mc.getNetworkHandler().getPlayerList().size();
                tr.draw(ms, "Players: " + players, x, y, 0xFFFFFF);
            }
        } else {
            tr.draw(ms, "Singleplayer", x, y, 0xAAAAAA);
        }
    }
}
