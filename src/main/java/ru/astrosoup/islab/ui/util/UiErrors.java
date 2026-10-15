package ru.astrosoup.islab.ui.util;

import ru.astrosoup.islab.exceptions.BusinessException;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class UiErrors {
    private static final Logger LOG = Logger.getLogger(UiErrors.class.getName());

    private UiErrors() {
    }

    public static void show(Throwable t) {
        Notifications.error(message(t));
    }

    public static String message(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause() == c ? null : c.getCause()) {
            if (c instanceof BusinessException) {
                return c.getMessage();
            }
        }
        LOG.log(Level.SEVERE, "Unexpected error", t);
        Throwable root = t;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return "Непредвиденная ошибка: " + (root.getMessage() == null ? root.getClass().getSimpleName() : root.getMessage());
    }
}
