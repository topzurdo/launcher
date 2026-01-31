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

    /**
     * Creates the ServerInfoModule and registers its HUD settings.
     *
     * Initializes the module with id "server_info", display name "Server Info",
     * description "Информация о сервере", and category HUD. Adds the following settings:
     * - "pos_x" (Позиция X): horizontal position, default 900, range 0–2000
     * - "pos_y" (Позиция Y): vertical position, default 380, range 0–2000
     * - "show_ping" (Пинг): toggle ping display, default true
     * - "show_players" (Игроки): toggle player count display, default true
     */
    public ServerInfoModule() {
        super("server_info", "Server Info", "Информация о сервере", Category.HUD);

        posX = addSetting(Setting.ofInt("pos_x", "Позиция X", "Горизонтальная позиция", 900, 0, 2000));
        posY = addSetting(Setting.ofInt("pos_y", "Позиция Y", "Вертикальная позиция", 380, 0, 2000));
        showPing = addSetting(Setting.ofBoolean("show_ping", "Пинг", "Показывать пинг", true));
        showPlayers = addSetting(Setting.ofBoolean("show_players", "Игроки", "Показывать кол-во игроков", true));
    }

    /**
     * Provides the HUD bounding rectangle for this module.
     *
     * @return an int array [x, y, width, height] representing the HUD bounds, where x and y are the configured horizontal and vertical positions
     */
    @Override
    public int[] getHudBounds() {
        return new int[] { posX.getValue(), posY.getValue(), 120, 40 };
    }

    /**
     * Renders server-related HUD information (server address, ping, and player count) at the configured X/Y position.
     *
     * If server info is available, draws the server address and, depending on settings, the player's ping and the current player count.
     * If no server info is available, displays "Singleplayer".
     * No rendering occurs when the module is disabled or when there is no active player.
     *
     * @param partialTicks partial render tick time used for interpolation during rendering
     */
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