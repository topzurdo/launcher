package com.topzurdo.launcher.ui.views;

import com.topzurdo.launcher.ui.components.StatusPill;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

/**
 * Window title bar: drag, server pill, minimize, close.
 */
public class TitleBar extends HBox {

    private final StatusPill serverPill;
    private final javafx.scene.control.Label wipeBanner;
    private Runnable onMinimize;
    private Runnable onClose;
    private Region authRegion;
    private boolean authVisible;

    public TitleBar() {
        getStyleClass().add("title-bar");
        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(8, 12, 8, 12));
        setSpacing(12);

        serverPill = new StatusPill();
        serverPill.setText("...");
        wipeBanner = new javafx.scene.control.Label("ВАЙП БЫЛ НЕДАВНО");
        wipeBanner.getStyleClass().add("title-wipe-banner");
        wipeBanner.setVisible(false);
        authRegion = new Region();
        authRegion.setVisible(false);
        authVisible = false;

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Button minBtn = new Button("—");
        minBtn.getStyleClass().add("title-btn");
        minBtn.setOnAction(e -> { if (onMinimize != null) onMinimize.run(); });
        Button closeBtn = new Button("✕");
        closeBtn.getStyleClass().add("title-btn-close");
        closeBtn.setOnAction(e -> { if (onClose != null) onClose.run(); });

        getChildren().addAll(serverPill, wipeBanner, authRegion, spacer, minBtn, closeBtn);
    }

    public StatusPill getServerPill() { return serverPill; }
    public void setOnMinimize(Runnable r) { this.onMinimize = r; }
    public void setOnClose(Runnable r) { this.onClose = r; }
    public void setAuthVisible(boolean v) {
        authVisible = v;
        if (authRegion != null) authRegion.setVisible(v);
    }

    /** Показывает/скрывает плашку «ВАЙП БЫЛ НЕДАВНО» (парсинг MOTD). */
    public void setWipeBannerVisible(boolean visible) {
        if (wipeBanner != null) wipeBanner.setVisible(visible);
    }
}
