package ru.astrosoup.islab.ui;

import com.vaadin.cdi.annotation.RouteScoped;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import ru.astrosoup.islab.entities.Coordinates;
import ru.astrosoup.islab.events.ChangeBroadcaster;
import ru.astrosoup.islab.services.CoordinatesService;
import ru.astrosoup.islab.ui.dialogs.CoordinatesFormDialog;
import ru.astrosoup.islab.ui.util.Confirm;
import ru.astrosoup.islab.ui.util.Notifications;
import ru.astrosoup.islab.ui.util.UiErrors;

@RouteScoped
@Route(value = "coordinates", layout = MainLayout.class)
@PageTitle("Координаты")
public class CoordinatesView extends VerticalLayout {

    @Inject
    CoordinatesService service;
    @Inject
    ChangeBroadcaster broadcaster;

    private final Grid<Coordinates> grid = new Grid<>();
    private Runnable unsubscribe;

    @PostConstruct
    void init() {
        setSizeFull();
        Button create = new Button("Создать координаты", VaadinIcon.PLUS.create(), e -> openForm(new Coordinates()));
        create.addThemeName("primary");

        grid.addThemeName("row-stripes");
        grid.addColumn(Coordinates::getId).setHeader("ID").setSortable(true).setFlexGrow(0);
        grid.addColumn(Coordinates::getX).setHeader("X").setSortable(true);
        grid.addColumn(Coordinates::getY).setHeader("Y").setSortable(true);
        grid.addComponentColumn(this::actions).setHeader("Действия").setFlexGrow(0).setWidth("140px");
        grid.setSizeFull();

        add(new HorizontalLayout(create), grid);
        expand(grid);
        reload();
    }

    private Component actions(Coordinates c) {
        Button edit = new Button(VaadinIcon.EDIT.create(), e -> {
            try {
                openForm(service.get(c.getId()));
            } catch (RuntimeException ex) {
                UiErrors.show(ex);
                reload();
            }
        });
        edit.setTooltipText("Изменить");
        Button delete = new Button(VaadinIcon.TRASH.create(), e -> Confirm.delete("Удаление координат",
                "Удалить координаты #" + c.getId() + "?", () -> {
                    try {
                        service.delete(c.getId());
                        Notifications.success("Координаты #" + c.getId() + " удалены");
                    } catch (RuntimeException ex) {
                        UiErrors.show(ex); // например, «координаты используются организациями»
                    }
                    reload();
                }));
        delete.setTooltipText("Удалить");
        delete.addThemeName("error");
        edit.addThemeName("tertiary");
        delete.addThemeName("tertiary");
        return new HorizontalLayout(edit, delete);
    }

    private void openForm(Coordinates bean) {
        new CoordinatesFormDialog(bean, service, saved -> reload()).open();
    }

    private void reload() {
        try {
            grid.setItems(service.findAll());
        } catch (RuntimeException ex) {
            UiErrors.show(ex);
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        UI ui = attachEvent.getUI();
        unsubscribe = broadcaster.register(event -> {
            try {
                ui.access(this::reload);
            } catch (RuntimeException ignored) {
                // UI уже закрыт
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
