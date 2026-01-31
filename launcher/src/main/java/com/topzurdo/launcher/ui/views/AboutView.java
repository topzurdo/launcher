package com.topzurdo.launcher.ui.views;

import com.topzurdo.launcher.config.theme.DesignTokens;
import com.topzurdo.launcher.TopZurdoLauncher;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

/**
 * О проекте — расширенная информация о лаунчере и моде.
 */
public class AboutView extends VBox {

    public AboutView() {
        getStyleClass().add("panel");
        setSpacing(DesignTokens.SPACING_24);
        setAlignment(Pos.TOP_CENTER);
        setMaxWidth(Double.MAX_VALUE);
        setPadding(new Insets(24, 32, 32, 32));

        Label title = new Label("О ПРОЕКТЕ");
        title.getStyleClass().add("panel-title");
        Label sub = new Label("TopZurdo Launcher " + TopZurdoLauncher.VERSION);
        sub.getStyleClass().add("panel-subtitle");
        Rectangle line = new Rectangle(320, 1);
        line.getStyleClass().add("gold-line");

        VBox content = new VBox(DesignTokens.SPACING_24);
        content.setAlignment(Pos.TOP_LEFT);
        content.setMaxWidth(520);

        Label desc = new Label(
            "Официальный лаунчер для клиента TopZurdo — премиального мода для Minecraft 1.16.5 на Fabric. "
            + "Лаунчер обеспечивает быструю установку игры, модов оптимизации и единый вход в ваш опыт.");
        desc.getStyleClass().add("about-desc");
        desc.setWrapText(true);

        Label sectionTech = new Label("ТЕХНОЛОГИИ");
        sectionTech.getStyleClass().add("about-section-title");
        Label tech = new Label(
            "• Minecraft 1.16.5 • Fabric Loader\n"
            + "• Моды оптимизации: Sodium, Lithium, Starlight, LazyDFU, FerriteCore и др.\n"
            + "• Мод TopZurdo: HUD, рендер, утилиты, премиальный UI в стиле золота и тёмной темы");
        tech.getStyleClass().add("about-body");
        tech.setWrapText(true);

        Label sectionFeatures = new Label("ВОЗМОЖНОСТИ ЛАУНЧЕРА");
        sectionFeatures.getStyleClass().add("about-section-title");
        Label features = new Label(
            "• Автоподбор RAM и профиля JVM под ваш ПК\n"
            + "• Одна кнопка — установка Minecraft и модов оптимизации\n"
            + "• Настройка сервера, разрешения, Java\n"
            + "• Тёмная премиальная тема в стиле мода");
        features.getStyleClass().add("about-body");
        features.setWrapText(true);

        Label sectionCredits = new Label("БЛАГОДАРНОСТИ");
        sectionCredits.getStyleClass().add("about-section-title");
        Label credits = new Label(
            "Fabric, Modrinth, сообщество Minecraft. Стиль: премиум тёмный + золотой акцент (Liquid Glass).");
        credits.getStyleClass().add("about-desc");
        credits.setWrapText(true);

        content.getChildren().addAll(
            desc,
            sectionTech, tech,
            sectionFeatures, features,
            sectionCredits, credits
        );

        ScrollPane scroll = new ScrollPane(content);
        scroll.getStyleClass().add("about-scroll");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setBackground(javafx.scene.layout.Background.EMPTY);
        scroll.setPadding(new Insets(0));

        getChildren().addAll(title, sub, line, scroll);
    }
}
