package ru.astrosoup.islab.ui;

import com.vaadin.cdi.annotation.RouteScoped;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import ru.astrosoup.islab.entities.Organization;
import ru.astrosoup.islab.entities.OrganizationType;
import ru.astrosoup.islab.events.ChangeBroadcaster;
import ru.astrosoup.islab.services.OperationsService;
import ru.astrosoup.islab.services.OrganizationService;
import ru.astrosoup.islab.ui.util.Notifications;
import ru.astrosoup.islab.ui.util.OrganizationPicker;
import ru.astrosoup.islab.ui.util.UiErrors;

import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;

@RouteScoped 
@Route(value = "operations", layout = MainLayout.class)
@PageTitle("Специальные операции")
public class OperationsView extends VerticalLayout {

    @Inject
    OperationsService operations;
    @Inject
    OrganizationService organizations;
    @Inject
    ChangeBroadcaster broadcaster;

    private OrganizationPicker mergeFirst;
    private OrganizationPicker mergeSecond;
    private OrganizationPicker absorbAcquirer;
    private OrganizationPicker absorbTarget;
    private Runnable unsubscribe;

    @PostConstruct
    void init() {
        setPadding(true);
        setMaxWidth("900px");

        add(averageCard(), postalCard(), typeCard(), mergeCard(), absorbCard());
    }


    private Component averageCard() {
        Span result = resultSpan();
        Button run = new Button("Рассчитать", e -> {
            try {
                OptionalDouble avg = operations.averageRating();
                result.setText(avg.isPresent()
                        ? "Средний рейтинг: " + String.format(Locale.US, "%.2f", avg.getAsDouble())
                        : "В системе нет организаций — среднее посчитать невозможно");
            } catch (RuntimeException ex) {
                UiErrors.show(ex);
            }
        });
        run.addThemeName("primary");
        return card("Среднее значение рейтинга",
                "Среднее арифметическое поля rating по всем организациям.", new HorizontalLayout(JustifyContentMode.CENTER, run), result);
    }


    private Component postalCard() {
        TextField street = new TextField("Улица почтового адреса");
        street.setWidth("320px");
        Checkbox none = new Checkbox("Почтовый адрес не задан (null)");
        none.addValueChangeListener(e -> street.setEnabled(!e.getValue()));
        Span result = resultSpan();
        Button run = new Button("Посчитать", e -> {
            try {
                long count;
                if (none.getValue()) {
                    count = operations.countWithoutPostalAddress();
                } else {
                    if (street.getValue() == null || street.getValue().isBlank()) {
                        street.setInvalid(true);
                        street.setErrorMessage("Введите улицу или отметьте «адрес не задан»");
                        return;
                    }
                    street.setInvalid(false);
                    count = operations.countByPostalAddress(street.getValue());
                }
                result.setText("Найдено организаций: " + count);
            } catch (RuntimeException ex) {
                UiErrors.show(ex);
            }
        });
        run.addThemeName("primary");
        HorizontalLayout row = new HorizontalLayout(JustifyContentMode.CENTER, street, none, run);
        row.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);
        return card("Количество организаций с заданным почтовым адресом",
                "Адреса сравниваются по значению (улице).", row, result);
    }


    private Component typeCard() {
        ComboBox<OrganizationType> type = new ComboBox<>("Тип организации");
        type.setItems(OrganizationType.values());
        type.setItemLabelGenerator(Enum::name);
        type.setWidth("320px");
        Span result = resultSpan();
        Button run = new Button("Посчитать", e -> {
            if (type.getValue() == null) {
                type.setInvalid(true);
                type.setErrorMessage("Выберите тип");
                return;
            }
            type.setInvalid(false);
            try {
                result.setText("Найдено организаций: " + operations.countByTypeGreaterThan(type.getValue()));
            } catch (RuntimeException ex) {
                UiErrors.show(ex);
            }
        });
        run.addThemeName("primary");
        HorizontalLayout row = new HorizontalLayout(JustifyContentMode.CENTER, type, run);
        row.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);
        return card("Количество организаций с типом больше заданного",
                "Порядок типов: " + String.join(" < ", java.util.Arrays.stream(OrganizationType.values()).map(Enum::name).toList())
                        + ". Организации без типа не учитываются.", row, result);
    }


    private Component mergeCard() {
        mergeFirst = new OrganizationPicker("Первая организация", organizations);
        mergeSecond = new OrganizationPicker("Вторая организация", organizations);
        TextField name = new TextField("Название новой организации");
        name.setWidthFull();
        TextField street = new TextField("Адрес новой организации (улица)");
        street.setWidthFull();
        Span result = resultSpan();

        Button run = new Button("Объединить", e -> {
            Organization a = mergeFirst.requireValue();
            Organization b = mergeSecond.requireValue();
            boolean ok = a != null && b != null;
            if (name.getValue() == null || name.getValue().isBlank()) {
                name.setInvalid(true);
                name.setErrorMessage("Укажите название новой организации");
                ok = false;
            } else {
                name.setInvalid(false);
            }
            if (street.getValue() == null || street.getValue().isBlank()) {
                street.setInvalid(true);
                street.setErrorMessage("Укажите адрес новой организации");
                ok = false;
            } else {
                street.setInvalid(false);
            }
            if (!ok) return;
            try {
                Organization merged = operations.merge(a.getId(), b.getId(), name.getValue(), street.getValue());
                result.setText("Создана организация #" + merged.getId() + " «" + merged.getName() + "»: сотрудников "
                        + merged.getEmployeesCount() + ". Исходные организации #" + a.getId() + " и #" + b.getId()
                        + " упразднены.");
                Notifications.success("Организации объединены");
                mergeFirst.clear();
                mergeSecond.clear();
                refreshPickers();
            } catch (RuntimeException ex) {
                UiErrors.show(ex);
            }
        });
        run.addThemeName("primary");
        return card("Объединение организаций",
                "Создаётся новая организация, в неё зачисляются все сотрудники двух исходных (оборот суммируется, "
                        + "рейтинг — среднее, координаты — от первой). Исходные организации упраздняются.",
                new HorizontalLayout(mergeFirst, mergeSecond), name, street, new HorizontalLayout(
                        JustifyContentMode.CENTER, run), result);
    }


    private Component absorbCard() {
        absorbAcquirer = new OrganizationPicker("Поглощающая организация", organizations);
        absorbTarget = new OrganizationPicker("Поглощаемая организация", organizations);
        Span result = resultSpan();

        Button run = new Button("Поглотить", e -> {
            Organization acquirer = absorbAcquirer.requireValue();
            Organization target = absorbTarget.requireValue();
            if (acquirer == null || target == null) return;
            try {
                Organization updated = operations.absorb(acquirer.getId(), target.getId());
                result.setText("Организация #" + updated.getId() + " поглотила #" + target.getId()
                        + ". Теперь в ней сотрудников: " + updated.getEmployeesCount() + " (без увольнений).");
                Notifications.success("Поглощение выполнено");
                absorbAcquirer.clear();
                absorbTarget.clear();
                refreshPickers();
            } catch (RuntimeException ex) {
                UiErrors.show(ex);
            }
        });
        run.addThemeName("primary");
        return card("Поглощение организации",
                "Все сотрудники (и оборот) поглощаемой организации переходят к поглощающей без увольнений; "
                        + "поглощаемая организация упраздняется.",
                new HorizontalLayout(absorbAcquirer, absorbTarget), new HorizontalLayout(JustifyContentMode.CENTER, run), result);
    }


    private void refreshPickers() {
        for (OrganizationPicker p : List.of(mergeFirst, mergeSecond, absorbAcquirer, absorbTarget)) {
            p.refresh();
        }
    }

    private static Span resultSpan() {
        Span s = new Span();
        s.getStyle().set("font-weight", "600");
        return s;
    }

    private static Div card(String title, String description, Component... content) {
        Div card = new Div();
        card.setWidthFull();
        card.getStyle()
                .set("box-sizing", "border-box")
                .set("padding", "var(--lumo-space-m)")
                .set("border", "1px solid var(--lumo-contrast-20pct, #ccc)")
                .set("border-radius", "var(--lumo-border-radius-l, 8px)")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "var(--lumo-space-s)");

        H3 heading = new H3(title);
        heading.getStyle().set("margin", "0");

        Paragraph p = new Paragraph(description);
        p.getStyle().set("color", "var(--lumo-secondary-text-color)").set("margin", "0");

        card.add(heading, p);
        card.add(content);
        return card;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        UI ui = attachEvent.getUI();
        unsubscribe = broadcaster.register(event -> {
            try {
                ui.access(this::refreshPickers);
            } catch (RuntimeException ignored) {
            }
        });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (unsubscribe != null) {
            unsubscribe.run();
            unsubscribe = null;
        }
    }
}
