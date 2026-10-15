package ru.astrosoup.islab.ui.dialogs;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import ru.astrosoup.islab.entities.Coordinates;
import ru.astrosoup.islab.services.CoordinatesService;
import ru.astrosoup.islab.ui.util.Notifications;
import ru.astrosoup.islab.ui.util.UiErrors;

import java.util.function.Consumer;

public class CoordinatesFormDialog extends Dialog {
    private final Binder<Coordinates> binder = new Binder<>();
    private final Coordinates bean;
    private final CoordinatesService service;
    private final Consumer<Coordinates> onSaved;

    public CoordinatesFormDialog(Coordinates bean, CoordinatesService service, Consumer<Coordinates> onSaved) {
        this.bean = bean;
        this.service = service;
        this.onSaved = onSaved;
        boolean isNew = bean.getId() == 0;
        setHeaderTitle(isNew ? "Новые координаты" : "Изменение координат #" + bean.getId());
        setCloseOnOutsideClick(false);

        NumberField x = new NumberField("X (максимум 580)");
        x.setMax(580);
        x.setRequiredIndicatorVisible(true);
        x.setWidthFull();
        IntegerField y = new IntegerField("Y (максимум 885)");
        y.setMax(885);
        y.setRequiredIndicatorVisible(true);
        y.setWidthFull();

        binder.forField(x)
                .asRequired("Введите координату X")
                .withValidator(v -> v <= 580, "Координата X не может быть больше 580")
                .withValidator(v -> !v.isNaN() && !v.isInfinite(), "Координата X должна быть конечным числом")
                .bind(Coordinates::getX, Coordinates::setX);
        binder.forField(y)
                .asRequired("Введите координату Y")
                .withValidator(v -> v <= 885, "Координата Y не может быть больше 885")
                .bind(Coordinates::getY, Coordinates::setY);

        if (isNew) {
            binder.readBean(null);
        } else {
            binder.readBean(bean);
        }

        FormLayout form = new FormLayout(x, y);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        add(form);

        Button save = new Button("Сохранить", e -> save());
        save.addThemeName("primary");
        getFooter().add(new Button("Отмена", e -> close()), save);
    }

    private void save() {
        if (!binder.validate().isOk()) {
            return;
        }
        try {
            binder.writeBean(bean);
            Coordinates saved = bean.getId() == 0 ? service.create(bean) : service.update(bean);
            close();
            Notifications.success("Координаты сохранены");
            onSaved.accept(saved);
        } catch (ValidationException ignored) {
        } catch (RuntimeException ex) {
            UiErrors.show(ex);
        }
    }
}
