package com.topzurdo.mod.gui;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.topzurdo.mod.gui.components.NeonButton;
import com.topzurdo.mod.gui.components.atoms.TextInput;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;

/**
 * Экран для смены ника в игре
 */
public class AccountSwitcherScreen extends Screen {

    private final Screen parent;
    private TextInput usernameField;
    private NeonButton saveButton;
    private NeonButton cancelButton;
    private String currentUsername = "Player";
    private String errorMessage = null;

    private static final int PANEL_W = 400;
    private static final int PANEL_H = 200;
    private static final int BTN_W = 150;
    private static final int BTN_H = 40;

    public AccountSwitcherScreen(Screen parent) {
        super(new LiteralText("Account Switcher"));
        this.parent = parent;
        loadCurrentUsername();
    }

    @Override
    protected void init() {
        super.init();

        int cx = width / 2;
        int cy = height / 2;
        int px = cx - PANEL_W / 2;
        int py = cy - PANEL_H / 2;

        // Поле ввода ника
        usernameField = new TextInput(
            px + 50, py + 60, PANEL_W - 100, 35,
            "Никнейм", // label
            currentUsername, // initialValue
            16, // maxLength
            s -> {} // onChange
        );

        // Кнопка сохранить
        saveButton = new NeonButton(
            cx - BTN_W - 10, py + PANEL_H - 60, BTN_W, BTN_H,
            new LiteralText("Сохранить"),
            OceanTheme.NEON_CYAN,
            b -> saveUsername()
        );
        addButton(saveButton);

        // Кнопка отмена
        cancelButton = new NeonButton(
            cx + 10, py + PANEL_H - 60, BTN_W, BTN_H,
            new LiteralText("Отмена"),
            0xFF57534E,
            b -> { if (client != null) client.openScreen(parent); }
        );
        addButton(cancelButton);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        // Фон
        UIRenderHelper.fillVerticalGradient(matrices, 0, 0, width, height, 0xFF0A0A18, OceanTheme.BG_DEEP);

        int cx = width / 2;
        int cy = height / 2;
        int px = cx - PANEL_W / 2;
        int py = cy - PANEL_H / 2;

        // Панель
        UIRenderHelper.fillRoundRect(matrices, px, py, PANEL_W, PANEL_H, 12, OceanTheme.BG_PANEL);

        // Рамка скруглённая
        UIRenderHelper.drawRoundBorder(matrices, px, py, PANEL_W, PANEL_H, 12, OceanTheme.NEON_CYAN);

        // Заголовок
        String title = "Смена ника";
        textRenderer.draw(matrices, title, cx - textRenderer.getWidth(title) / 2f, py + 20, 0xFFFFFFFF);

        // Текущий ник
        String currentText = "Текущий ник: " + currentUsername;
        textRenderer.draw(matrices, currentText, px + 50, py + 45, OceanTheme.TEXT_MUTED);

        // Рендер поля ввода
        if (usernameField != null) {
            usernameField.render(matrices, mouseX, mouseY);
        }

        // Сообщение об ошибке
        if (errorMessage != null) {
            textRenderer.draw(matrices, errorMessage, px + 50, py + 110, 0xFFFF4444);
        }

        // Подсказка
        String hint = "3-16 символов: буквы, цифры, подчёркивание";
        textRenderer.draw(matrices, hint, px + 50, py + 130, OceanTheme.TEXT_MUTED);

        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean charTyped(char chr, int keyCode) {
        if (usernameField != null && usernameField.charTyped(chr, keyCode)) {
            return true;
        }
        return super.charTyped(chr, keyCode);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (usernameField != null && usernameField.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (keyCode == 256) { // ESC
            if (client != null) client.openScreen(parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (usernameField != null) {
            // Проверяем клик внутри поля
            int fx = usernameField.getX();
            int fy = usernameField.getY();
            int fw = usernameField.getWidth();
            int fh = usernameField.getHeight();
            boolean clickedInside = mouseX >= fx && mouseX <= fx + fw &&
                                   mouseY >= fy && mouseY <= fy + fh;
            usernameField.setFocused(clickedInside);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void loadCurrentUsername() {
        try {
            // Путь к конфигу лаунчера
            String userHome = System.getProperty("user.home");
            Path configPath = Paths.get(userHome, ".topzurdo", "launcher.json");

            if (Files.exists(configPath)) {
                StringBuilder json = new StringBuilder();
                try (BufferedReader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        json.append(line).append("\n");
                    }
                }
                JsonObject config = new Gson().fromJson(json.toString(), JsonObject.class);
                if (config != null && config.has("username")) {
                    currentUsername = config.get("username").getAsString();
                    if (currentUsername == null || currentUsername.isEmpty()) {
                        currentUsername = "Player";
                    }
                }
            }
        } catch (Exception e) {
            // Игнорируем ошибки
        }
    }

    private void saveUsername() {
        String newUsername = usernameField.getText().trim();

        // Валидация
        if (newUsername.length() < 3 || newUsername.length() > 16) {
            errorMessage = "Ник должен быть от 3 до 16 символов";
            return;
        }

        if (!newUsername.matches("^[a-zA-Z0-9_]+$")) {
            errorMessage = "Ник может содержать только буквы, цифры и _";
            return;
        }

        try {
            // Сохраняем в конфиг лаунчера
            String userHome = System.getProperty("user.home");
            Path configDir = Paths.get(userHome, ".topzurdo");
            Files.createDirectories(configDir);
            Path configPath = configDir.resolve("launcher.json");

            JsonObject config;
            if (Files.exists(configPath)) {
                StringBuilder json = new StringBuilder();
                try (BufferedReader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        json.append(line).append("\n");
                    }
                }
                config = new Gson().fromJson(json.toString(), JsonObject.class);
                if (config == null) {
                    config = new JsonObject();
                }
            } else {
                config = new JsonObject();
            }

            config.addProperty("username", newUsername);

            try (BufferedWriter writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
                writer.write(new Gson().toJson(config));
            }

            // Обновляем текущий ник
            currentUsername = newUsername;
            errorMessage = null;

            // Показываем сообщение об успехе
            if (client != null && client.player != null) {
                client.player.sendMessage(new LiteralText("§aНик изменён на: " + newUsername), false);
            }

            // Закрываем экран через небольшую задержку
            if (client != null) {
                client.openScreen(parent);
            }

        } catch (IOException e) {
            errorMessage = "Ошибка сохранения: " + e.getMessage();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
