package com.topzurdo.launcher.ui.views;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Side nav: Home, Settings, About.
 */
public class NavRail extends VBox {

    private final Button homeBtn;
    private final Button settingsBtn;
    private final Button aboutBtn;
    private final Label versionLabel;

    public NavRail() {
        getStyleClass().add("nav-rail");
        setAlignment(Pos.TOP_CENTER);
        setSpacing(8);
        setPadding(new Insets(24, 12, 12, 12));

        homeBtn = new Button("Главная");
        homeBtn.getStyleClass().add("nav-btn");
        settingsBtn = new Button("Настройки");
        settingsBtn.getStyleClass().add("nav-btn");
        aboutBtn = new Button("О проекте");
        aboutBtn.getStyleClass().add("nav-btn");

        versionLabel = new Label("v1.0.0");
        versionLabel.getStyleClass().add("nav-version");
        VBox.setMargin(versionLabel, new Insets(24, 0, 0, 0));

        getChildren().addAll(homeBtn, settingsBtn, aboutBtn, versionLabel);
    }

    public Button getHome() { return homeBtn; }
    public Button getSettings() { return settingsBtn; }
    public Button getAbout() { return aboutBtn; }
    public void setActive(String id) {
        homeBtn.getStyleClass().remove("nav-btn-active");
        settingsBtn.getStyleClass().remove("nav-btn-active");
        aboutBtn.getStyleClass().remove("nav-btn-active");
        if ("home".equals(id)) homeBtn.getStyleClass().add("nav-btn-active");
        else if ("settings".equals(id)) settingsBtn.getStyleClass().add("nav-btn-active");
        else if ("about".equals(id)) aboutBtn.getStyleClass().add("nav-btn-active");
    }
    public void setVersion(String text) { versionLabel.setText(text); }
}
