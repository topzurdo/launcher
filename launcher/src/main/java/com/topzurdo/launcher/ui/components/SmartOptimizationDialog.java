package com.topzurdo.launcher.ui.components;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Модальное окно «Подбор под мой ПК» с визуализацией прогресса:
 * Сканирование CPU → Анализ RAM → Подбор JVM.
 * ~1.5 сек, затем применяются настройки.
 */
public class SmartOptimizationDialog extends Dialog<Void> {

    private static final String[] STEPS = {
        "Сканирование CPU...",
        "Анализ доступной RAM...",
        "Подбор аргументов JVM..."
    };
    private static final int STEP_MS = 500;

    private final ProgressBar progressBar;
    private final Label stepLabel;
    private final Label javaBitLabel;

    public SmartOptimizationDialog() {
        setTitle("Подбор настроек");
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(320);
        progressBar.getStyleClass().add("smart-opt-progress");
        stepLabel = new Label(STEPS[0]);
        stepLabel.getStyleClass().add("smart-opt-step");
        javaBitLabel = new Label(detectJavaBitInfo());
        javaBitLabel.getStyleClass().add("smart-opt-hint");

        DialogPane pane = getDialogPane();
        pane.getButtonTypes().add(ButtonType.CLOSE);
        pane.setContent(createContent());
    }

    private Node createContent() {
        VBox box = new VBox(16);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20, 24, 16, 24));
        box.setPrefWidth(360);
        Label title = new Label("Подбор настроек под ваш ПК");
        title.getStyleClass().add("smart-opt-title");
        box.getChildren().addAll(title, stepLabel, progressBar, javaBitLabel);
        return box;
    }

    /** Запускает анимацию шагов и по завершении вызывает onComplete на FX thread. */
    public void runScan(Runnable onComplete) {
        DialogPane pane = getDialogPane();
        Node closeBtn = pane.lookupButton(ButtonType.CLOSE);
        if (closeBtn != null) closeBtn.setDisable(true);
        stepLabel.setText(STEPS[0]);
        progressBar.setProgress(0);
        javaBitLabel.setText(detectJavaBitInfo());

        final int totalSteps = STEPS.length;
        final int[] step = { 0 };

        Timeline timeline = new Timeline();
        for (int i = 0; i < totalSteps; i++) {
            final int idx = i;
            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(STEP_MS * (i + 1)), e -> {
                step[0] = idx + 1;
                if (idx + 1 < totalSteps) {
                    stepLabel.setText(STEPS[idx + 1]);
                }
                progressBar.setProgress((double) (idx + 1) / totalSteps);
            }));
        }
        timeline.getKeyFrames().add(new KeyFrame(Duration.millis(STEP_MS * totalSteps + 100), e -> {
            stepLabel.setText("Готово!");
            progressBar.setProgress(1.0);
            if (closeBtn != null) closeBtn.setDisable(false);
            if (onComplete != null) onComplete.run();
        }));
        timeline.play();
    }

    private static String detectJavaBitInfo() {
        String arch = System.getProperty("sun.arch.data.model", "");
        String osArch = System.getProperty("os.arch", "").toLowerCase();
        boolean is64 = "64".equals(arch) || osArch.contains("64");
        if (!is64) {
            return "Внимание: обнаружена 32-битная Java. Для лучшей работы установите 64-битную Java.";
        }
        return "64-битная Java — ок.";
    }
}
