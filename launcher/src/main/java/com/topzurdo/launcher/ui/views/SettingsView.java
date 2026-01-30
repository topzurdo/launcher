package com.topzurdo.launcher.ui.views;

import com.topzurdo.launcher.config.JvmProfile;
import com.topzurdo.launcher.config.theme.DesignTokens;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * Settings panel: RAM, Java, resolution, version, server, checkboxes, save/reset.
 */
public class SettingsView extends VBox {

    private final Slider ramSlider;
    private final Label ramLabel;
    private final TextField javaPathField;
    private final ComboBox<String> resolutionCombo;
    private final ComboBox<String> versionCombo;
    private final TextField serverField;
    private final TextField portField;
    private final CheckBox autoConnectCheck;
    private final CheckBox fullscreenCheck;
    private final CheckBox effectsCheck;
    private final CheckBox fabricDebugCheck;
    private final CheckBox lightThemeCheck;
    private final CheckBox autoRamCheck;
    private final ComboBox<String> jvmProfileCombo;
    private final Button installOptModsBtn;
    private final Button browseJavaBtn;
    private final Button saveBtn;
    private final Button resetBtn;

    // Visual effects
    private final CheckBox customColorCheck;
    private final ColorPicker preferredColorPicker;

    public SettingsView() {
        getStyleClass().add("panel");
        setSpacing(DesignTokens.SPACING_24);
        setAlignment(Pos.TOP_CENTER);
        setMaxWidth(Double.MAX_VALUE);

        Label title = new Label("ПЕРСОНАЛИЗАЦИЯ");
        title.getStyleClass().add("panel-title");
        Label sub = new Label("Настройте опыт под себя");
        sub.getStyleClass().add("panel-subtitle");
        Rectangle line = new Rectangle(300, 1);
        line.getStyleClass().add("gold-line");

        VBox form = new VBox(DesignTokens.SPACING_32);
        form.setAlignment(Pos.TOP_LEFT);
        form.setPadding(DesignTokens.insets(DesignTokens.SPACING_24, DesignTokens.SPACING_24));

        HBox ramRow = new HBox(DesignTokens.SPACING_24);
        ramRow.setAlignment(Pos.CENTER_LEFT);
        ramSlider = new Slider(1024, 16384, 4096);
        ramSlider.setPrefWidth(400);
        ramSlider.getStyleClass().add("gold-slider");
        ramLabel = new Label("4096 MB");
        ramLabel.getStyleClass().add("ram-value");
        ramRow.getChildren().addAll(ramSlider, ramLabel);
        autoRamCheck = new CheckBox("Автоопределение RAM (50–70% памяти)");
        autoRamCheck.getStyleClass().add("gold-check");
        VBox ramContent = new VBox(DesignTokens.SPACING_12);
        ramContent.getChildren().addAll(ramRow, autoRamCheck);
        VBox ram = section("ВЫДЕЛЕНИЕ РЕСУРСОВ", ramContent, "Рекомендуем 4-8 GB. Авто — 50–70% доступной памяти");

        jvmProfileCombo = new ComboBox<>();
        jvmProfileCombo.setPrefWidth(220);
        jvmProfileCombo.getStyleClass().add("gold-combo");
        for (JvmProfile p : JvmProfile.values()) {
            jvmProfileCombo.getItems().add(p.getDisplayName());
        }
        jvmProfileCombo.setValue(JvmProfile.MEDIUM.getDisplayName());
        VBox jvmProfile = section("ПРОФИЛЬ JVM", jvmProfileCombo, "Слабый / Средний / Мощный — флаги G1GC под ваш ПК");

        HBox javaRow = new HBox(DesignTokens.SPACING_12);
        javaRow.setAlignment(Pos.CENTER_LEFT);
        javaPathField = new TextField();
        javaPathField.setPrefWidth(400);
        javaPathField.getStyleClass().add("settings-field");
        javaPathField.setPromptText("Автоопределение...");
        browseJavaBtn = new Button("ОБЗОР");
        browseJavaBtn.getStyleClass().add("browse-btn");
        javaRow.getChildren().addAll(javaPathField, browseJavaBtn);
        VBox java = section("ОКРУЖЕНИЕ JAVA", javaRow, null);

        VBox res = section("РАЗРЕШЕНИЕ ДИСПЛЕЯ",
            resolutionCombo = new ComboBox<>() {{
                setPrefWidth(220);
                getStyleClass().add("gold-combo");
            }},
            null
        );

        VBox ver = section("ВЕРСИЯ MINECRAFT",
            versionCombo = new ComboBox<>() {{
                setPrefWidth(220);
                getStyleClass().add("gold-combo");
            }},
            "Выберите предпочтительную версию"
        );

        HBox serverRow = new HBox(DesignTokens.SPACING_12);
        serverRow.setAlignment(Pos.CENTER_LEFT);
        serverField = new TextField();
        serverField.setPrefWidth(280);
        serverField.getStyleClass().add("settings-field");
        serverField.setPromptText("mc.server.com");
        portField = new TextField();
        portField.setPrefWidth(100);
        portField.getStyleClass().add("settings-field");
        portField.setPromptText("25565");
        serverRow.getChildren().addAll(serverField, portField);

        HBox presetRow = new HBox(DesignTokens.SPACING_12);
        presetRow.setAlignment(Pos.CENTER_LEFT);
        Button presetFunTimeBtn = new Button("FunTime");
        presetFunTimeBtn.getStyleClass().add("preset-btn");
        presetFunTimeBtn.setPrefHeight(36);
        presetFunTimeBtn.setOnAction(e -> {
            serverField.setText("mc.funtime.su");
            portField.setText("25565");
        });
        Button presetOtherBtn = new Button("Другие");
        presetOtherBtn.getStyleClass().add("preset-btn");
        presetOtherBtn.setPrefHeight(36);
        presetOtherBtn.setOnAction(e -> {
            serverField.clear();
            portField.clear();
        });
        presetRow.getChildren().addAll(presetFunTimeBtn, presetOtherBtn);

        VBox serverSection = new VBox(DesignTokens.SPACING_12);
        serverSection.getChildren().addAll(serverRow, presetRow);
        VBox srv = section("СЕРВЕР", serverSection, "Быстрый выбор: FunTime или свой сервер");

        installOptModsBtn = new Button("◆ Установить моды оптимизации");
        installOptModsBtn.getStyleClass().add("preset-btn");
        installOptModsBtn.setPrefHeight(40);
        VBox optMods = section("ОПТИМИЗАЦИЯ", installOptModsBtn,
            "FerriteCore (память −30%) + Francium (Random) — прирост FPS, ускорение загрузки");

        autoConnectCheck = new CheckBox("Автоматическое подключение к серверу");
        autoConnectCheck.getStyleClass().add("gold-check");
        fullscreenCheck = new CheckBox("Полноэкранный режим");
        fullscreenCheck.getStyleClass().add("gold-check");
        effectsCheck = new CheckBox("Визуальные эффекты лаунчера");
        effectsCheck.getStyleClass().add("gold-check");
        effectsCheck.setSelected(true);
        fabricDebugCheck = new CheckBox("Расширенное логирование Fabric (для отладки модов)");
        fabricDebugCheck.getStyleClass().add("gold-check");
        lightThemeCheck = new CheckBox("Светлая тема");
        lightThemeCheck.getStyleClass().add("gold-check");
        VBox opts = section("ОПЦИИ КОМФОРТА", new VBox(DesignTokens.SPACING_16, autoConnectCheck, fullscreenCheck, effectsCheck, fabricDebugCheck, lightThemeCheck), null);

        // Visual effects section
        customColorCheck = new CheckBox("Использовать свой цвет для частиц и эффектов");
        customColorCheck.getStyleClass().add("gold-check");

        preferredColorPicker = new ColorPicker(Color.web("#22D3EE"));
        preferredColorPicker.getStyleClass().add("gold-color-picker");
        preferredColorPicker.setPrefWidth(150);
        preferredColorPicker.setDisable(true);

        customColorCheck.setOnAction(e -> preferredColorPicker.setDisable(!customColorCheck.isSelected()));

        HBox colorRow = new HBox(DesignTokens.SPACING_12);
        colorRow.setAlignment(Pos.CENTER_LEFT);
        colorRow.getChildren().addAll(customColorCheck, preferredColorPicker);

        VBox visualEffects = section("ВИЗУАЛЬНЫЕ ЭФФЕКТЫ", colorRow,
            "Выберите цвет для частиц, облаков и эффектов в игре");

        saveBtn = new Button("СОХРАНИТЬ");
        saveBtn.getStyleClass().add("save-btn");
        saveBtn.setPrefSize(180, 50);
        resetBtn = new Button("СБРОСИТЬ");
        resetBtn.getStyleClass().add("reset-btn");
        resetBtn.setPrefSize(140, 50);
        HBox btns = new HBox(DesignTokens.SPACING_24, saveBtn, resetBtn);
        btns.setAlignment(Pos.CENTER);

        form.getChildren().addAll(ram, jvmProfile, optMods, java, res, ver, srv, opts, visualEffects, btns);

        ScrollPane scroll = new ScrollPane(form);
        scroll.getStyleClass().add("settings-scroll");
        scroll.setFitToWidth(true);
        scroll.setMaxHeight(Double.MAX_VALUE);

        getChildren().addAll(title, sub, line, scroll);
    }

    private VBox section(String title, javafx.scene.Node content, String hint) {
        VBox v = new VBox(DesignTokens.SPACING_12);
        v.getStyleClass().add("settings-section");
        Label l = new Label(title);
        l.getStyleClass().add("section-title");
        v.getChildren().add(l);
        v.getChildren().add(content);
        if (hint != null) {
            Label h = new Label(hint);
            h.getStyleClass().add("hint-text");
            v.getChildren().add(h);
        }
        return v;
    }

    public Slider getRamSlider() { return ramSlider; }
    public Label getRamLabel() { return ramLabel; }
    public TextField getJavaPathField() { return javaPathField; }
    public Button getBrowseJavaButton() { return browseJavaBtn; }
    public ComboBox<String> getResolutionCombo() { return resolutionCombo; }
    public ComboBox<String> getVersionCombo() { return versionCombo; }
    public TextField getServerField() { return serverField; }
    public TextField getPortField() { return portField; }
    public CheckBox getAutoConnectCheck() { return autoConnectCheck; }
    public CheckBox getFullscreenCheck() { return fullscreenCheck; }
    public CheckBox getEffectsCheck() { return effectsCheck; }
    public CheckBox getFabricDebugCheck() { return fabricDebugCheck; }
    public CheckBox getLightThemeCheck() { return lightThemeCheck; }
    public CheckBox getAutoRamCheck() { return autoRamCheck; }
    public ComboBox<String> getJvmProfileCombo() { return jvmProfileCombo; }
    public Button getInstallOptModsButton() { return installOptModsBtn; }
    public Button getSaveButton() { return saveBtn; }
    public Button getResetButton() { return resetBtn; }

    public CheckBox getCustomColorCheck() { return customColorCheck; }
    public ColorPicker getPreferredColorPicker() { return preferredColorPicker; }

    /**
     * Get preferred color as RGB integer.
     */
    public int getPreferredColorRGB() {
        Color c = preferredColorPicker.getValue();
        int r = (int) (c.getRed() * 255);
        int g = (int) (c.getGreen() * 255);
        int b = (int) (c.getBlue() * 255);
        return (r << 16) | (g << 8) | b;
    }

    /**
     * Set preferred color from RGB integer.
     */
    public void setPreferredColorRGB(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        preferredColorPicker.setValue(Color.rgb(r, g, b));
    }
}
