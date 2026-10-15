package ru.astrosoup.islab.ui.util;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import ru.astrosoup.islab.entities.Organization;
import ru.astrosoup.islab.repositories.OrganizationFilter;
import ru.astrosoup.islab.services.OrganizationService;

import java.util.List;

/** Выпадающий список организаций с ленивой подгрузкой и поиском по части названия. */
public class OrganizationPicker extends ComboBox<Organization> {
    private final CallbackDataProvider<Organization, String> provider;

    public OrganizationPicker(String label, OrganizationService service) {
        super(label);
        provider = new CallbackDataProvider<>(
                query -> service.findPage(OrganizationFilter.byName(query.getFilter().orElse("")),
                        List.of(), query.getOffset(), query.getLimit()).stream(),
                query -> (int) service.count(OrganizationFilter.byName(query.getFilter().orElse(""))));
        setItems(provider);
        setItemLabelGenerator(o -> "#" + o.getId() + " — " + o.getName());
        setClearButtonVisible(true);
        setWidthFull();
        setPlaceholder("Начните вводить название…");
    }

    public void refresh() {
        provider.refreshAll();
    }

    /** @return выбранная организация либо null (с пометкой ошибки у поля) */
    public Organization requireValue() {
        Organization value = getValue();
        if (value == null) {
            setInvalid(true);
            setErrorMessage("Выберите организацию");
        } else {
            setInvalid(false);
        }
        return value;
    }
}
