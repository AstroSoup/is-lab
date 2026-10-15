package ru.astrosoup.islab.ui.dialogs;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import ru.astrosoup.islab.entities.Address;
import ru.astrosoup.islab.entities.Organization;

import java.text.SimpleDateFormat;

public class OrganizationInfoDialog extends Dialog {

    public OrganizationInfoDialog(Organization o, Runnable onEdit) {
        setHeaderTitle("Организация #" + o.getId());
        setWidth("560px");
        setMaxWidth("96vw");

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(false);

        content.add(row("ID", String.valueOf(o.getId())));
        content.add(row("Название", o.getName()));
        content.add(row("Полное название", o.getFullName()));
        content.add(row("Тип", o.getType() == null ? null : o.getType().name()));
        content.add(row("Дата создания", o.getCreationDate() == null ? null
                : new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(o.getCreationDate())));
        content.add(row("Годовой оборот", String.valueOf(o.getAnnualTurnover())));
        content.add(row("Сотрудников", String.valueOf(o.getEmployeesCount())));
        content.add(row("Рейтинг", String.valueOf(o.getRating())));

        content.add(new H4("Связанные объекты"));
        content.add(row("Координаты",
                "#" + o.getCoordinates().getId() + ": x=" + o.getCoordinates().getX() + ", y=" + o.getCoordinates().getY()));
        content.add(row("Официальный адрес", describe(o.getOfficialAddress())));
        content.add(row("Почтовый адрес", describe(o.getPostalAddress())));
        add(content);

        Button edit = new Button("Изменить", VaadinIcon.EDIT.create(), e -> {
            close();
            onEdit.run();
        });
        edit.addThemeName("primary");
        getFooter().add(new Button("Закрыть", e -> close()), edit);
    }

    private static String describe(Address a) {
        if (a == null) return null;
        return "#" + a.getId() + (a.getStreet() == null ? " (улица не указана)" : ": " + a.getStreet());
    }

    private static Div row(String label, String value) {
        Span l = new Span(label);
        l.getStyle().set("flex", "0 0 180px").set("color", "var(--lumo-secondary-text-color)");
        Span v = new Span(value == null || value.isBlank() ? "—" : value);
        Div row = new Div(l, v);
        row.getStyle().set("display", "flex").set("padding", "4px 0");
        row.setWidthFull();
        return row;
    }
}
