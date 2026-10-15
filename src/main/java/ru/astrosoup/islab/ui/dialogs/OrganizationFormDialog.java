package ru.astrosoup.islab.ui.dialogs;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import ru.astrosoup.islab.entities.Address;
import ru.astrosoup.islab.entities.Coordinates;
import ru.astrosoup.islab.entities.Organization;
import ru.astrosoup.islab.entities.OrganizationType;
import ru.astrosoup.islab.exceptions.EntityValidationException;
import ru.astrosoup.islab.services.AddressService;
import ru.astrosoup.islab.services.CoordinatesService;
import ru.astrosoup.islab.services.OrganizationService;
import ru.astrosoup.islab.ui.util.BlankToNullConverter;
import ru.astrosoup.islab.ui.util.LongConverter;
import ru.astrosoup.islab.ui.util.Notifications;
import ru.astrosoup.islab.ui.util.UiErrors;

import java.util.function.Consumer;


public class OrganizationFormDialog extends Dialog {
    private final Binder<Organization> binder = new Binder<>();
    private final Organization bean;
    private final OrganizationService organizations;
    private final CoordinatesService coordinatesService;
    private final AddressService addressService;
    private final Consumer<Organization> onSaved;

    private final ComboBox<Coordinates> coordinatesBox = new ComboBox<>("Координаты *");
    private final ComboBox<Address> officialBox = new ComboBox<>("Официальный адрес");
    private final ComboBox<Address> postalBox = new ComboBox<>("Почтовый адрес");

    public OrganizationFormDialog(Organization bean, OrganizationService organizations,
                                  CoordinatesService coordinatesService, AddressService addressService,
                                  Consumer<Organization> onSaved) {
        this.bean = bean;
        this.organizations = organizations;
        this.coordinatesService = coordinatesService;
        this.addressService = addressService;
        this.onSaved = onSaved;

        boolean isNew = bean.getId() == 0;
        setHeaderTitle(isNew ? "Создание организации" : "Изменение организации #" + bean.getId());
        setWidth("760px");
        setMaxWidth("96vw");
        setCloseOnOutsideClick(false);

        TextField name = new TextField("Название *");
        name.setMaxLength(255);
        TextField fullName = new TextField("Полное название");
        fullName.setMaxLength(500);

        TextField turnover = new TextField("Годовой оборот * (целое > 0)");
        turnover.setAllowedCharPattern("[0-9]");
        turnover.setMaxLength(19);
        IntegerField employees = new IntegerField("Количество сотрудников * (> 0)");
        employees.setMin(1);
        employees.setStepButtonsVisible(true);
        IntegerField rating = new IntegerField("Рейтинг * (> 0)");
        rating.setMin(1);
        rating.setStepButtonsVisible(true);

        ComboBox<OrganizationType> type = new ComboBox<>("Тип организации");
        type.setItems(OrganizationType.values());
        type.setItemLabelGenerator(Enum::name);
        type.setClearButtonVisible(true);

        configureCoordinatesBox();
        configureAddressBox(officialBox);
        configureAddressBox(postalBox);
        reloadCoordinates();
        reloadAddresses();

        bindFields(name, fullName, turnover, employees, rating, type);

        if (isNew) {
            binder.readBean(null);
        } else {
            binder.readBean(bean);
        }

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("560px", 2));
        form.add(name, fullName);
        form.add(withAddButton(coordinatesBox, e -> openNewCoordinates()), 2);
        form.add(withAddButton(officialBox, e -> openNewAddress(officialBox)),
                withAddButton(postalBox, e -> openNewAddress(postalBox)));
        form.add(turnover, employees, rating, type);
        add(form);

        Button save = new Button("Сохранить", e -> save());
        save.addThemeName("primary");
        getFooter().add(new Button("Отмена", e -> close()), save);
    }

    private void bindFields(TextField name, TextField fullName, TextField turnover, IntegerField employees,
                            IntegerField rating, ComboBox<OrganizationType> type) {
        binder.forField(name)
                .asRequired("Название организации не может быть пустым")
                .withValidator(s -> !s.isBlank(), "Название организации не может состоять только из пробелов")
                .bind(Organization::getName, Organization::setName);
        binder.forField(fullName)
                .withConverter(new BlankToNullConverter())
                .bind(Organization::getFullName, Organization::setFullName);
        binder.forField(coordinatesBox)
                .asRequired("Выберите координаты (или создайте новые кнопкой «+»)")
                .bind(Organization::getCoordinates, Organization::setCoordinates);
        binder.forField(officialBox)
                .bind(Organization::getOfficialAddress, Organization::setOfficialAddress);
        binder.forField(postalBox)
                .bind(Organization::getPostalAddress, Organization::setPostalAddress);
        binder.forField(turnover)
                .asRequired("Введите годовой оборот")
                .withConverter(new LongConverter())
                .withValidator(v -> v > 0, "Годовой оборот должен быть больше 0")
                .bind(Organization::getAnnualTurnover, Organization::setAnnualTurnover);
        binder.forField(employees)
                .asRequired("Введите количество сотрудников")
                .withValidator(v -> v > 0, "Количество сотрудников должно быть больше 0")
                .bind(Organization::getEmployeesCount, Organization::setEmployeesCount);
        binder.forField(rating)
                .asRequired("Введите рейтинг")
                .withValidator(v -> v > 0, "Рейтинг должен быть больше 0")
                .bind(Organization::getRating, Organization::setRating);
        binder.forField(type)
                .bind(Organization::getType, Organization::setType);
    }

    private void configureCoordinatesBox() {
        coordinatesBox.setItemLabelGenerator(c -> "#" + c.getId() + ": x=" + c.getX() + ", y=" + c.getY());
        coordinatesBox.setWidthFull();
        coordinatesBox.setRequiredIndicatorVisible(true);
        coordinatesBox.setPlaceholder("Выберите из существующих");
    }

    private void configureAddressBox(ComboBox<Address> box) {
        box.setItemLabelGenerator(a -> "#" + a.getId() + ": " + (a.getStreet() == null ? "(улица не указана)" : a.getStreet()));
        box.setClearButtonVisible(true);
        box.setWidthFull();
        box.setPlaceholder("Не задан");
    }

    private void reloadCoordinates() {
        coordinatesBox.setItems(coordinatesService.findAll());
    }

    private void reloadAddresses() {
        var all = addressService.findAll();
        officialBox.setItems(all);
        postalBox.setItems(all);
    }

    private HorizontalLayout withAddButton(ComboBox<?> box,
                                           com.vaadin.flow.component.ComponentEventListener<com.vaadin.flow.component.ClickEvent<Button>> listener) {
        Button add = new Button(VaadinIcon.PLUS.create(), listener);
        add.setTooltipText("Создать новый объект и выбрать его");
        HorizontalLayout row = new HorizontalLayout(box, add);
        row.setWidthFull();
        row.setPadding(false);
        row.setAlignItems(FlexComponent.Alignment.END);
        row.expand(box);
        return row;
    }

    private void openNewCoordinates() {
        new CoordinatesFormDialog(new Coordinates(), coordinatesService, saved -> {
            reloadCoordinates();
            coordinatesBox.setValue(saved);
        }).open();
    }

    private void openNewAddress(ComboBox<Address> target) {
        new AddressFormDialog(new Address(), addressService, saved -> {
            Address official = officialBox.getValue();
            Address postal = postalBox.getValue();
            reloadAddresses();
            officialBox.setValue(official);
            postalBox.setValue(postal);
            target.setValue(saved);
        }).open();
    }

    private void save() {
        if (!binder.validate().isOk()) {
            Notifications.error("Исправьте ошибки в выделенных полях");
            return;
        }
        try {
            binder.writeBean(bean);
            Organization saved = bean.getId() == 0 ? organizations.create(bean) : organizations.update(bean);
            close();
            Notifications.success("Организация сохранена (id " + saved.getId() + ")");
            onSaved.accept(saved);
        } catch (ValidationException ignored) {
            // сообщения уже показаны у полей
        } catch (EntityValidationException ex) {
            Notifications.error("Некорректные данные: " + String.join("; ", ex.getViolations()));
        } catch (RuntimeException ex) {
            UiErrors.show(ex);
        }
    }
}
