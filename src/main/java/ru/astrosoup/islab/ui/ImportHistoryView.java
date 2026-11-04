package ru.astrosoup.islab.ui;

import com.vaadin.cdi.annotation.RouteScoped;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import ru.astrosoup.islab.entities.ImportOperation;
import ru.astrosoup.islab.entities.ImportStatus;
import ru.astrosoup.islab.events.ChangeBroadcaster;
import ru.astrosoup.islab.events.EntityKind;
import ru.astrosoup.islab.services.ImportHistoryService;
import ru.astrosoup.islab.ui.util.UiErrors;

import java.text.SimpleDateFormat;
import java.util.List;

/** История импорта: обычный пользователь видит только свои операции, администратор — все. */
@RouteScoped
@Route(value = "import-history", layout = MainLayout.class)
@PageTitle("История импорта")
public class ImportHistoryView extends VerticalLayout {

    @Inject
    ImportHistoryService history;
    @Inject
    ChangeBroadcaster broadcaster;

    private Runnable unsubscribe;
    private final Grid<ImportOperation> grid = new Grid<>();
    private CallbackDataProvider<ImportOperation, Void> dataProvider;
    private String user = "";

    @PostConstruct
    void init() {
        setSizeFull();
        setPadding(true);
        user = UserSession.currentUser().orElse("");

        Span scope = new Span();
        scope.getStyle().set("color", "var(--lumo-secondary-text-color)");
        try {
            scope.setText(history.seesAllOperations(user)
                    ? "Администратор: показаны операции всех пользователей"
                    : "Показаны только ваши операции импорта");
        } catch (RuntimeException ex) {
            UiErrors.show(ex);
        }

        grid.addThemeName("row-stripes");
        grid.addColumn(ImportOperation::getId).setHeader("ID операции").setFlexGrow(0).setAutoWidth(true);
        grid.addComponentColumn(ImportHistoryView::statusBadge).setHeader("Статус").setFlexGrow(0).setAutoWidth(true);
        grid.addColumn(ImportOperation::getUserName).setHeader("Пользователь").setAutoWidth(true);
        grid.addColumn(op -> op.getStatus() == ImportStatus.SUCCESS ? String.valueOf(op.getAddedCount()) : "—")
                .setHeader("Добавлено объектов").setFlexGrow(0).setAutoWidth(true);
        grid.addColumn(op -> new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(op.getCreatedAt()))
                .setHeader("Время").setAutoWidth(true);
        grid.addColumn(op -> op.getFileName() == null ? "" : op.getFileName()).setHeader("Файл").setAutoWidth(true);
        grid.addComponentColumn(this::details).setHeader("Ошибки").setFlexGrow(0).setAutoWidth(true);
        grid.setSizeFull();

        dataProvider = new CallbackDataProvider<>(
                query -> history.findPage(user, query.getOffset(), query.getLimit()).stream(),
                query -> (int) Math.min(Integer.MAX_VALUE, history.count(user)));
        grid.setItems(dataProvider);

        add(scope, grid);
        expand(grid);
    }

    private static Component statusBadge(ImportOperation op) {
        Span badge = new Span(op.getStatus().label());
        badge.getElement().getThemeList().addAll(List.of("badge",
                op.getStatus() == ImportStatus.SUCCESS ? "success" : "error"));
        return badge;
    }

    private Component details(ImportOperation op) {
        if (op.getStatus() != ImportStatus.FAILED || op.getErrorMessage() == null) {
            return new Span();
        }
        Button open = new Button("Показать", e -> {
            Dialog dialog = new Dialog();
            dialog.setHeaderTitle("Операция #" + op.getId() + ": причины неудачи");
            Div body = new Div();
            body.getStyle().set("max-height", "60vh").set("overflow", "auto");
            for (String line : op.getErrorMessage().split("\n")) {
                body.add(new Div(line));
            }
            dialog.add(body);
            dialog.getFooter().add(new Button("Закрыть", ev -> dialog.close()));
            dialog.open();
        });
        open.addThemeName("tertiary");
        return open;
    }

    private void reload() {
        try {
            dataProvider.refreshAll();
        } catch (RuntimeException ex) {
            UiErrors.show(ex);
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        UI ui = attachEvent.getUI();
        unsubscribe = broadcaster.register(event -> {
            if (event.kind() != EntityKind.IMPORT_HISTORY) {
                return;
            }
            try {
                ui.access(this::reload);
            } catch (RuntimeException ignored) {
                // окно уже закрыто
            }
        });
        reload();
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (unsubscribe != null) {
            unsubscribe.run();
            unsubscribe = null;
        }
    }
}
