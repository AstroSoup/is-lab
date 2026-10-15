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
import ru.astrosoup.islab.entities.Address;
import ru.astrosoup.islab.events.ChangeBroadcaster;
import ru.astrosoup.islab.services.AddressService;
import ru.astrosoup.islab.ui.dialogs.AddressFormDialog;
import ru.astrosoup.islab.ui.util.Confirm;
import ru.astrosoup.islab.ui.util.Notifications;
import ru.astrosoup.islab.ui.util.UiErrors;

@RouteScoped
@Route(value = "addresses", layout = MainLayout.class)
@PageTitle("Адреса")
public class AddressesView extends VerticalLayout {

    @Inject
    AddressService service;
    @Inject
    ChangeBroadcaster broadcaster;

    private final Grid<Address> grid = new Grid<>();
    private Runnable unsubscribe;

    @PostConstruct
    void init() {
        setSizeFull();
        Button create = new Button("Создать адрес", VaadinIcon.PLUS.create(), e -> openForm(new Address()));
        create.addThemeName("primary");

        grid.addThemeName("row-stripes");
        grid.addColumn(Address::getId).setHeader("ID").setSortable(true).setFlexGrow(0);
        grid.addColumn(a -> a.getStreet() == null ? "" : a.getStreet()).setHeader("Улица").setSortable(true);
        grid.addComponentColumn(this::actions).setHeader("Действия").setFlexGrow(0).setWidth("140px");
        grid.setSizeFull();

        add(new HorizontalLayout(create), grid);
        expand(grid);
        reload();
    }

    private Component actions(Address a) {
        Button edit = new Button(VaadinIcon.EDIT.create(), e -> {
            try {
                openForm(service.get(a.getId()));
            } catch (RuntimeException ex) {
                UiErrors.show(ex);
                reload();
            }
        });
        edit.setTooltipText("Изменить");
        Button delete = new Button(VaadinIcon.TRASH.create(), e -> Confirm.delete("Удаление адреса",
                "Удалить адрес #" + a.getId() + "?", () -> {
                    try {
                        service.delete(a.getId());
                        Notifications.success("Адрес #" + a.getId() + " удалён");
                    } catch (RuntimeException ex) {
                        UiErrors.show(ex);
                    }
                    reload();
                }));
        delete.setTooltipText("Удалить");
        delete.addThemeName("error");
        edit.addThemeName("tertiary");
        delete.addThemeName("tertiary");
        return new HorizontalLayout(edit, delete);
    }

    private void openForm(Address bean) {
        new AddressFormDialog(bean, service, saved -> reload()).open();
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
