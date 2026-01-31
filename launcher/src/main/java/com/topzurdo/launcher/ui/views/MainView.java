package com.topzurdo.launcher.ui.views;

import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

/**
 * Main layout: title bar top, nav rail left, content center.
 */
public class MainView extends BorderPane {

    private final TitleBar titleBar;
    private final NavRail navRail;
    private final StackPane contentPane;

    public MainView() {
        getStyleClass().add("main-view");
        titleBar = new TitleBar();
        navRail = new NavRail();
        contentPane = new StackPane();
        contentPane.getStyleClass().add("content-pane");

        setTop(titleBar);
        setLeft(navRail);
        setCenter(contentPane);
    }

    public void setContent(Node node) {
        contentPane.getChildren().clear();
        if (node != null) contentPane.getChildren().add(node);
    }

    public TitleBar getTitleBar() { return titleBar; }
    public NavRail getNavRail() { return navRail; }
}
