package com.topzurdo.launcher.ui.views;

import com.topzurdo.launcher.config.theme.DesignTokens;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

/**
 * Home panel: welcome, login (MS + offline), play, progress, status, stat boxes.
 */
public class HomeView extends VBox {

    private final Label welcomeTitle;
    private final VBox microsoftLoginBox;
    private final Button microsoftLoginBtn;
    private final TextField usernameField;
    private final Button playButton;
    private final VBox progressSection;
    private final ProgressBar downloadProgress;
    private final Label progressPercentLabel;
    private final Label statusLabel;

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

        usernameField = new TextField() {{
            setPromptText("Player");
            getStyleClass().add("username-field");
            setPrefSize(350, 50);
            setText("Player"); // Дефолтный ник
            setVisible(false); // Скрываем поле
        }};

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

        playSection.getChildren().addAll(playButton, progressSection, statusLabel);

        HBox stats = new HBox(DesignTokens.SPACING_64);
        stats.setAlignment(Pos.CENTER);
        for (String[] t : new String[][]{
            {"СВОБОДА", "Без ограничений"},
            {"ВЕРСИЯ", "1.16.5 Forge"},
            {"УТОНЧЁННОСТЬ", "Визуалы премиум"}
        }) {
            VBox b = new VBox(6);
            b.setAlignment(Pos.CENTER);
            b.getStyleClass().add("stat-box");
            b.getChildren().addAll(
                new Label("◆") {{ getStyleClass().add("stat-icon"); }},
                new Label(t[0]) {{ getStyleClass().add("stat-title"); }},
                new Label(t[1]) {{ getStyleClass().add("stat-value"); }}
            );
            stats.getChildren().add(b);
        }

        getChildren().addAll(welcome, line, playSection, stats);
    }

    public Label getWelcomeTitle() { return welcomeTitle; }
    public VBox getMicrosoftLoginBox() { return microsoftLoginBox; }
    public Button getMicrosoftLoginBtn() { return microsoftLoginBtn; }
    public TextField getUsernameField() { return usernameField; }
    public Button getPlayButton() { return playButton; }
    public VBox getProgressSection() { return progressSection; }
    public ProgressBar getDownloadProgress() { return downloadProgress; }
    public Label getProgressPercentLabel() { return progressPercentLabel; }
    public Label getStatusLabel() { return statusLabel; }

    public void setMicrosoftLoginVisible(boolean v) { microsoftLoginBox.setVisible(v); }
    public void setUsername(String s) { usernameField.setText(s != null ? s : ""); }
    public void setUsernameEditable(boolean e) { usernameField.setDisable(!e); }
}
