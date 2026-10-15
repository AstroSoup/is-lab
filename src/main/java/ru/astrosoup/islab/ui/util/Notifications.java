package ru.astrosoup.islab.ui.util;

import com.vaadin.flow.component.notification.Notification;

public final class Notifications {
    private Notifications() {
    }

    public static void success(String message) {
        Notification n = Notification.show(message, 3500, Notification.Position.TOP_END);
        n.addThemeName("success");
    }

    public static void error(String message) {
        Notification n = Notification.show(message, 8000, Notification.Position.TOP_END);
        n.addThemeName("error");
    }

    public static void info(String message) {
        Notification.show(message, 4000, Notification.Position.TOP_END);
    }
}
