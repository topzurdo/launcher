package com.topzurdo.launcher.ui.components;

import javafx.scene.control.Label;

/**
 * Small status pill (e.g. server online/offline).
 */
public class StatusPill extends Label {

    public enum Status { LOADING, OK, ERROR }

    private Status status = Status.LOADING;

    public StatusPill() {
        getStyleClass().add("status-pill");
        setText("...");
    }

    public void setStatus(Status status) {
        this.status = status;
        getStyleClass().removeAll("status-pill-loading", "status-pill-ok", "status-pill-error");
        switch (status) {
            case LOADING: getStyleClass().add("status-pill-loading"); break;
            case OK: getStyleClass().add("status-pill-ok"); break;
            case ERROR: getStyleClass().add("status-pill-error"); break;
        }
    }

    public Status getStatus() { return status; }
}
