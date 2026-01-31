package com.topzurdo.launcher.ui.views;

import com.topzurdo.launcher.config.theme.DesignTokens;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import java.util.List;

/**
 * Home panel: welcome, login (MS + offline), play, progress, status, stat boxes.
 */
public class HomeView extends VBox {

    private final Label welcomeTitle;
    private final VBox microsoftLoginBox;
    private final Button microsoftLoginBtn;
    private final ComboBox<String> nicknameCombo;
    private final TextField usernameField; // kept for getUsernameField() compatibility; same content as combo editor
    private final Button playButton;
    private final VBox progressSection;
    private final ProgressBar downloadProgress;
    private final Label progressPercentLabel;
    private final Label statusLabel;
    private final Label statVersionLabel;

    public HomeView() {
        getStyleClass().add("panel");
        setSpacing(DesignTokens.SPACING_32);
        setAlignment(Pos.CENTER);
        setMaxWidth(Double.MAX_VALUE);

        VBox welcome = new VBox(DesignTokens.SPACING_12);
        welcome.setAlignment(Pos.CENTER);
        welcomeTitle = new Label("ДОБРО ПОЖАЛОВАТЬ В КЛУБ");
        welcomeTitle.getStyleClass().add("welcome-title");
        welcome.getChildren().addAll(welcomeTitle, new Label("Избранный опыт для истинных ценителей") {{
            getStyleClass().add("welcome-subtitle");
        }});

        HBox line = new HBox();
        line.setAlignment(Pos.CENTER);
        Rectangle goldLine = new Rectangle(450, 1);
        goldLine.getStyleClass().add("gold-line");
        line.getChildren().add(goldLine);

        // Убрали секцию авторизации - теперь только кнопка запуска
        // Создаем скрытые поля для совместимости с кодом
        microsoftLoginBox = new VBox(DesignTokens.SPACING_12);
        microsoftLoginBox.setVisible(false);
        microsoftLoginBox.getChildren().add(
            microsoftLoginBtn = new Button("◆ Войти через Microsoft") {{
                getStyleClass().add("microsoft-btn");
                setPrefSize(300, 50);
            }}
        );

        nicknameCombo = new ComboBox<>();
        nicknameCombo.setEditable(true);
        nicknameCombo.setPromptText("Player");
        nicknameCombo.getStyleClass().add("username-field");
        nicknameCombo.setPrefSize(350, 50);
        nicknameCombo.setValue("Player");
        usernameField = new TextField();
        usernameField.setVisible(false);

        VBox playSection = new VBox(DesignTokens.SPACING_24);
        playSection.setAlignment(Pos.CENTER);
        playButton = new Button("ПРИСТУПИТЬ К ИГРЕ");
        playButton.getStyleClass().add("play-btn");
        playButton.setPrefSize(320, 75);

        progressSection = new VBox(DesignTokens.SPACING_8);
        progressSection.setAlignment(Pos.CENTER);
        progressSection.setVisible(false);
        downloadProgress = new ProgressBar(0);
        downloadProgress.setPrefSize(400, 10);
        downloadProgress.getStyleClass().add("download-progress");
        progressPercentLabel = new Label("0%");
        progressPercentLabel.getStyleClass().add("progress-percent");
        progressSection.getChildren().addAll(downloadProgress, progressPercentLabel);

        statusLabel = new Label("Ваш приватный опыт ожидает");
        statusLabel.getStyleClass().add("status-label");

        playSection.getChildren().addAll(nicknameCombo, playButton, progressSection, statusLabel);

        statVersionLabel = new Label("Fabric 1.16.5");
        statVersionLabel.getStyleClass().add("stat-value");
        HBox stats = new HBox(DesignTokens.SPACING_64);
        stats.setAlignment(Pos.CENTER);
        String[][] statRows = {
            {"СВОБОДА", "Без ограничений"},
            {"ВЕРСИЯ", null},
            {"УТОНЧЁННОСТЬ", "Визуалы премиум"}
        };
        for (String[] t : statRows) {
            VBox b = new VBox(6);
            b.setAlignment(Pos.CENTER);
            b.getStyleClass().add("stat-box");
            Label valueLabel = "ВЕРСИЯ".equals(t[0]) ? statVersionLabel : new Label(t[1]);
            if (!"ВЕРСИЯ".equals(t[0])) valueLabel.getStyleClass().add("stat-value");
            b.getChildren().addAll(
                new Label("◆") {{ getStyleClass().add("stat-icon"); }},
                new Label(t[0]) {{ getStyleClass().add("stat-title"); }},
                valueLabel
            );
            stats.getChildren().add(b);
        }

        getChildren().addAll(welcome, line, playSection, stats);
    }

    public Label getWelcomeTitle() { return welcomeTitle; }
    public VBox getMicrosoftLoginBox() { return microsoftLoginBox; }
    public Button getMicrosoftLoginBtn() { return microsoftLoginBtn; }
    /** Для совместимости: возвращает поле, синхронизированное с ником (скрыто). Текущий ник — getNicknameText(). */
    public TextField getUsernameField() {
        String t = getNicknameText();
        usernameField.setText(t != null ? t : "");
        return usernameField;
    }
    public ComboBox<String> getNicknameCombo() { return nicknameCombo; }
    /** Текущий введённый/выбранный ник. */
    public String getNicknameText() {
        String v = nicknameCombo.getEditor().getText();
        if (v != null && !(v = v.trim()).isEmpty()) return v;
        v = nicknameCombo.getValue();
        return v != null && !v.isEmpty() ? v : "Player";
    }
    /** Устанавливает историю ников (последние 3). */
    public void setNicknameHistory(List<String> history) {
        if (history != null) nicknameCombo.getItems().setAll(history);
    }
    public Button getPlayButton() { return playButton; }
    public VBox getProgressSection() { return progressSection; }
    public ProgressBar getDownloadProgress() { return downloadProgress; }
    public Label getProgressPercentLabel() { return progressPercentLabel; }
    public Label getStatusLabel() { return statusLabel; }

    public void setMicrosoftLoginVisible(boolean v) { microsoftLoginBox.setVisible(v); }
    public void setUsername(String s) {
        String v = s != null ? s.trim() : "";
        if (v.isEmpty()) v = "Player";
        nicknameCombo.setValue(v);
        nicknameCombo.getEditor().setText(v);
    }
    public void setUsernameEditable(boolean e) { usernameField.setDisable(!e); }
    /** Устанавливает текст версии в блоке статистики (напр. "Fabric 1.16.5"). */
    public void setVersionStat(String text) {
        if (statVersionLabel != null) statVersionLabel.setText(text != null ? text : "—");
    }
}
