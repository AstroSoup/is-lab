package ru.astrosoup.islab.ui.dialogs;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import ru.astrosoup.islab.entities.Address;
import ru.astrosoup.islab.services.AddressService;
import ru.astrosoup.islab.ui.util.BlankToNullConverter;
import ru.astrosoup.islab.ui.util.Notifications;
import ru.astrosoup.islab.ui.util.UiErrors;

import java.util.function.Consumer;

public class AddressFormDialog extends Dialog {
    private final Binder<Address> binder = new Binder<>();
    private final Address bean;
    private final AddressService service;
    private final Consumer<Address> onSaved;

    public AddressFormDialog(Address bean, AddressService service, Consumer<Address> onSaved) {
        this.bean = bean;
        this.service = service;
        this.onSaved = onSaved;
        boolean isNew = bean.getId() == 0;
        setHeaderTitle(isNew ? "Новый адрес" : "Изменение адреса #" + bean.getId());
        setCloseOnOutsideClick(false);

        TextField street = new TextField("Улица (необязательно)");
        street.setWidth("360px");
        binder.forField(street)
                .withConverter(new BlankToNullConverter())
                .bind(Address::getStreet, Address::setStreet);
        binder.readBean(bean);
        add(street);

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
            Address saved = bean.getId() == 0 ? service.create(bean) : service.update(bean);
            close();
            Notifications.success("Адрес сохранён");
            onSaved.accept(saved);
        } catch (ValidationException ignored) {
        } catch (RuntimeException ex) {
            UiErrors.show(ex);
        }
    }
}
