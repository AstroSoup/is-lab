package ru.astrosoup.islab.ui.util;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;

/** Диалог подтверждения опасного действия (удаления). */
public final class Confirm {
    private Confirm() {
    }

    public static void delete(String header, String text, Runnable onConfirm) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(header);
        dialog.add(new Span(text));
        Button confirm = new Button("Удалить", e -> {
            dialog.close();
            onConfirm.run();
        });
        confirm.addThemeName("primary");
        confirm.addThemeName("error");
        Button cancel = new Button("Отмена", e -> dialog.close());
        dialog.getFooter().add(cancel, confirm);
        dialog.open();
    }
}
