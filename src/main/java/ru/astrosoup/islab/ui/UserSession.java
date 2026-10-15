package ru.astrosoup.islab.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinSession;

import java.util.Optional;

public final class UserSession {
    private static final String ATTRIBUTE = "ru.astrosoup.islab.user";

    private UserSession() {
    }

    public static Optional<String> currentUser() {
        VaadinSession session = VaadinSession.getCurrent();
        return session == null ? Optional.empty() : Optional.ofNullable((String) session.getAttribute(ATTRIBUTE));
    }

    public static boolean isLoggedIn() {
        return currentUser().isPresent();
    }

    public static void login(String name) {
        VaadinSession.getCurrent().setAttribute(ATTRIBUTE, name);
    }

    public static void logout() {
        UI ui = UI.getCurrent();
        VaadinSession session = VaadinSession.getCurrent();
        ui.getPage().setLocation("login");
        session.getSession().invalidate();
    }
}
