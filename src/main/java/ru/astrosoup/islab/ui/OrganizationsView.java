package ru.astrosoup.islab.ui;

import com.vaadin.cdi.annotation.RouteScoped;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.provider.QuerySortOrder;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import ru.astrosoup.islab.entities.Address;
import ru.astrosoup.islab.entities.Organization;
import ru.astrosoup.islab.events.ChangeBroadcaster;
import ru.astrosoup.islab.repositories.OrganizationFilter;
import ru.astrosoup.islab.repositories.SortSpec;
import ru.astrosoup.islab.services.AddressService;
import ru.astrosoup.islab.services.CoordinatesService;
import ru.astrosoup.islab.services.OrganizationService;
import ru.astrosoup.islab.ui.dialogs.OrganizationFormDialog;
import ru.astrosoup.islab.ui.dialogs.OrganizationInfoDialog;
import ru.astrosoup.islab.ui.util.Confirm;
import ru.astrosoup.islab.ui.util.Notifications;
import ru.astrosoup.islab.ui.util.UiErrors;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.stream.Stream;

@RouteScoped 
@Route(value = "", layout = MainLayout.class)
@PageTitle("Организации")
public class OrganizationsView extends VerticalLayout {

    @Inject
    OrganizationService organizations;
    @Inject
    CoordinatesService coordinatesService;
    @Inject
    AddressService addressService;
    @Inject
    ChangeBroadcaster broadcaster;

    private final Grid<Organization> grid = new Grid<>();
    private CallbackDataProvider<Organization, Void> dataProvider;

    private final TextField nameFilter = new TextField();
    private final TextField fullNameFilter = new TextField();
    private final TextField officialFilter = new TextField();
    private final TextField postalFilter = new TextField();
    private final TextField typeFilter = new TextField();

    private final Select<Integer> pageSizeSelect = new Select<>();
    private final Button firstPage = new Button(VaadinIcon.ANGLE_DOUBLE_LEFT.create());
    private final Button prevPage = new Button(VaadinIcon.ANGLE_LEFT.create());
    private final Button nextPage = new Button(VaadinIcon.ANGLE_RIGHT.create());
    private final Button lastPage = new Button(VaadinIcon.ANGLE_DOUBLE_RIGHT.create());
    private final Span pageInfo = new Span();

    private int page = 0;
    private int pageSize = 10;
    private long total = 0;
    private Runnable unsubscribe;

    @PostConstruct
    void init() {
        setSizeFull();
        setPadding(true);

        add(buildToolbar(), buildFilters(), buildGrid(), buildPager());
        expand(grid);
    }


    private Component buildToolbar() {
        Button create = new Button("Создать организацию", VaadinIcon.PLUS.create(), e -> openCreate());
        create.addThemeName("primary");
        Button byId = new Button("Найти по ID", VaadinIcon.SEARCH.create(), e -> openLookup());
        HorizontalLayout bar = new HorizontalLayout(create, byId);
        bar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        return bar;
    }

    private Component buildFilters() {
        configureFilter(nameFilter, "Название");
        configureFilter(fullNameFilter, "Полное название");
        configureFilter(officialFilter, "Официальный адрес");
        configureFilter(postalFilter, "Почтовый адрес");
        configureFilter(typeFilter, "Тип");
        Button clear = new Button("Сбросить", VaadinIcon.CLOSE_SMALL.create(), e -> {
            nameFilter.clear();
            fullNameFilter.clear();
            officialFilter.clear();
            postalFilter.clear();
            typeFilter.clear();
        });
        HorizontalLayout filters = new HorizontalLayout(nameFilter, fullNameFilter, officialFilter,
                postalFilter, typeFilter, clear);
        filters.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);
        filters.setWidthFull();
        filters.getStyle().set("flex-wrap", "wrap");
        return filters;
    }

    private void configureFilter(TextField field, String placeholder) {
        field.setPlaceholder(placeholder);
        field.setClearButtonVisible(true);
        field.setValueChangeMode(ValueChangeMode.LAZY);
        field.setValueChangeTimeout(400);
        field.setPrefixComponent(VaadinIcon.FILTER.create());
        field.addValueChangeListener(e -> {
            page = 0;
            safeReload();
        });
    }

    private Grid<Organization> buildGrid() {
        grid.addThemeName("row-stripes");
        grid.addThemeName("compact");
        grid.setSizeFull();
        grid.setMultiSort(false);

        column(Organization::getId, "ID", "id");
        column(Organization::getName, "Название", "name");
        column(o -> o.getCoordinates().getX(), "Координата X", "x");
        column(o -> o.getCoordinates().getY(), "Координата Y", "y");
        column(o -> o.getCreationDate() == null ? "" : new SimpleDateFormat("dd.MM.yyyy HH:mm").format(o.getCreationDate()),
                "Дата создания", "creationDate");
        column(o -> street(o.getOfficialAddress()), "Официальный адрес", "officialAddress");
        column(Organization::getAnnualTurnover, "Годовой оборот", "annualTurnover");
        column(Organization::getEmployeesCount, "Сотрудников", "employeesCount");
        column(Organization::getRating, "Рейтинг", "rating");
        column(o -> o.getFullName() == null ? "" : o.getFullName(), "Полное название", "fullName");
        column(o -> o.getType() == null ? "" : o.getType().name(), "Тип", "type");
        column(o -> street(o.getPostalAddress()), "Почтовый адрес", "postalAddress");

        grid.addComponentColumn(this::actions).setHeader("Действия").setFlexGrow(0).setWidth("170px");

        dataProvider = new CallbackDataProvider<>(
                query -> {
                    int limit = Math.min(query.getLimit(), pageSize - query.getOffset());
                    if (limit <= 0) {
                        return Stream.<Organization>empty();
                    }
                    int offset = page * pageSize + query.getOffset();
                    List<SortSpec> sort = query.getSortOrders().stream().map(this::toSortSpec).toList();
                    return organizations.findPage(currentFilter(), sort, offset, limit).stream();
                },
                query -> (int) Math.max(0, Math.min(pageSize, total - (long) page * pageSize)));
        grid.setItems(dataProvider);
        return grid;
    }

    private void column(ValueProvider<Organization, ?> value, String header, String sortKey) {
        grid.addColumn(value)
                .setHeader(header)
                .setSortProperty(sortKey)
                .setSortable(true)
                .setResizable(true)
                .setAutoWidth(true);
    }

    private SortSpec toSortSpec(QuerySortOrder order) {
        return new SortSpec(order.getSorted(), order.getDirection() == SortDirection.ASCENDING);
    }

    private Component actions(Organization o) {
        Button view = new Button(VaadinIcon.EYE.create(), e -> openInfo(o.getId()));
        view.setTooltipText("Просмотр");
        Button edit = new Button(VaadinIcon.EDIT.create(), e -> openEdit(o.getId()));
        edit.setTooltipText("Изменить");
        Button delete = new Button(VaadinIcon.TRASH.create(), e -> confirmDelete(o));
        delete.setTooltipText("Удалить");
        delete.addThemeName("error");
        for (Button b : List.of(view, edit, delete)) {
            b.addThemeName("tertiary");
            b.addThemeName("small");
        }
        return new HorizontalLayout(view, edit, delete);
    }

    private Component buildPager() {
        pageSizeSelect.setLabel("Строк на странице");
        pageSizeSelect.setItems(5, 10, 20, 50);
        pageSizeSelect.setValue(pageSize);
        pageSizeSelect.setWidth("170px");
        pageSizeSelect.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                pageSize = e.getValue();
                page = 0;
                safeReload();
            }
        });
        firstPage.addClickListener(e -> goTo(0));
        prevPage.addClickListener(e -> goTo(page - 1));
        nextPage.addClickListener(e -> goTo(page + 1));
        lastPage.addClickListener(e -> goTo(pageCount() - 1));

        HorizontalLayout pager = new HorizontalLayout(pageSizeSelect, firstPage, prevPage, pageInfo, nextPage, lastPage);
        pager.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);
        pager.setAlignItems(FlexComponent.Alignment.CENTER);
        return pager;
    }

    private OrganizationFilter currentFilter() {
        return new OrganizationFilter(nameFilter.getValue(), fullNameFilter.getValue(), officialFilter.getValue(),
                postalFilter.getValue(), typeFilter.getValue());
    }

    private int pageCount() {
        return (int) Math.max(1, (total + pageSize - 1) / pageSize);
    }

    private void goTo(int newPage) {
        page = Math.max(0, Math.min(newPage, pageCount() - 1));
        safeReload();
    }

    private void safeReload() {
        try {
            total = organizations.count(currentFilter());
            page = Math.max(0, Math.min(page, pageCount() - 1));
            pageInfo.setText("Страница " + (page + 1) + " из " + pageCount() + " · всего записей: " + total);
            firstPage.setEnabled(page > 0);
            prevPage.setEnabled(page > 0);
            nextPage.setEnabled(page < pageCount() - 1);
            lastPage.setEnabled(page < pageCount() - 1);
            dataProvider.refreshAll();
        } catch (RuntimeException ex) {
            UiErrors.show(ex);
        }
    }

    private void openCreate() {
        new OrganizationFormDialog(new Organization(), organizations, coordinatesService, addressService,
                saved -> safeReload()).open();
    }

    private void openEdit(long id) {
        try {
            Organization fresh = organizations.get(id); 
            new OrganizationFormDialog(fresh, organizations, coordinatesService, addressService,
                    saved -> safeReload()).open();
        } catch (RuntimeException ex) {
            UiErrors.show(ex);
            safeReload();
        }
    }

    private void openInfo(long id) {
        try {
            Organization fresh = organizations.get(id);
            new OrganizationInfoDialog(fresh, () -> openEdit(id)).open();
        } catch (RuntimeException ex) {
            UiErrors.show(ex);
            safeReload();
        }
    }

    private void openLookup() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Получение информации об организации по ID");
        TextField idField = new TextField("ID организации");
        idField.setAllowedCharPattern("[0-9]");
        idField.setMaxLength(18);
        idField.setAutofocus(true);
        idField.setWidth("320px");
        dialog.add(idField);

        Button find = new Button("Найти", e -> {
            String text = idField.getValue() == null ? "" : idField.getValue().trim();
            if (text.isEmpty() || Long.parseLong(text) <= 0) {
                idField.setInvalid(true);
                idField.setErrorMessage("Введите положительное целое число");
                return;
            }
            try {
                long id = Long.parseLong(text);
                Organization found = organizations.find(id).orElse(null);
                if (found == null) {
                    idField.setInvalid(true);
                    idField.setErrorMessage("Организация с ID " + id + " не найдена");
                    return;
                }
                dialog.close();
                new OrganizationInfoDialog(found, () -> openEdit(id)).open();
            } catch (RuntimeException ex) {
                UiErrors.show(ex);
            }
        });
        find.addThemeName("primary");
        find.addClickShortcut(com.vaadin.flow.component.Key.ENTER);
        dialog.getFooter().add(new Button("Отмена", e -> dialog.close()), find);
        dialog.open();
    }

    private void confirmDelete(Organization o) {
        Confirm.delete("Удаление организации",
                "Удалить организацию #" + o.getId() + " «" + o.getName() + "»? Действие необратимо.",
                () -> {
                    try {
                        organizations.delete(o.getId());
                        Notifications.success("Организация #" + o.getId() + " удалена");
                        safeReload();
                    } catch (RuntimeException ex) {
                        UiErrors.show(ex);
                        safeReload();
                    }
                });
    }

    private static String street(Address a) {
        return a == null || a.getStreet() == null ? "" : a.getStreet();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        UI ui = attachEvent.getUI();
        unsubscribe = broadcaster.register(event -> {
            try {
                ui.access(this::safeReload);
            } catch (RuntimeException ignored) {
            }
        });
        safeReload();
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (unsubscribe != null) {
            unsubscribe.run();
            unsubscribe = null;
        }
    }
}
