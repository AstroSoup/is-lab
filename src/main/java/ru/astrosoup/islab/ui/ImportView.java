package ru.astrosoup.islab.ui;

import com.vaadin.cdi.annotation.RouteScoped;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.UploadHandler;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import ru.astrosoup.islab.entities.OrganizationType;
import ru.astrosoup.islab.imports.OrganizationCsvParser;
import ru.astrosoup.islab.services.ImportResult;
import ru.astrosoup.islab.services.ImportService;
import ru.astrosoup.islab.ui.util.Notifications;
import ru.astrosoup.islab.ui.util.UiErrors;

import java.util.Arrays;

@RouteScoped
@Route(value = "import", layout = MainLayout.class)
@PageTitle("Импорт организаций")
public class ImportView extends VerticalLayout {

    private static final String EXAMPLE = """
            name,fullName,coordinatesX,coordinatesY,annualTurnover,employeesCount,rating,type,officialAddress,postalAddress
            Ромашка,ООО Ромашка,120.5,300,5000000,40,4,PRIVATE_LIMITED_COMPANY,"ул. Ленина, 1","ул. Мира, 5"
            Вектор,,10,20,1000000,12,2,COMMERCIAL,,
            """;

    @Inject
    ImportService importService;

    private final Button run = new Button("Импортировать", VaadinIcon.UPLOAD.create());
    private final Span selected = new Span("Файл не выбран");
    private final Div result = new Div();

    private volatile byte[] content;
    private volatile String fileName;

    @PostConstruct
    void init() {
        setPadding(true);
        setMaxWidth("900px");

        Upload upload = new Upload(UploadHandler.inMemory((metadata, data) -> onUploaded(metadata.fileName(), data)));
        upload.setAcceptedFileTypes(".csv", "text/csv");
        upload.setMaxFiles(1);
        upload.setMaxFileSize(ImportService.MAX_FILE_BYTES);
        upload.addFileRejectedListener(e -> Notifications.error(e.getErrorMessage()));
        upload.getElement().addEventListener("file-remove", e -> forgetFile());

        run.addThemeName("primary");
        run.setEnabled(false);
        run.addClickListener(e -> runImport());

        result.setWidthFull();

        add(new H3("Массовое добавление организаций из CSV"), formatHelp(), upload, selected, run, result);
    }

    private Component formatHelp() {
        String required = String.join(", ", Arrays.stream(OrganizationCsvParser.Column.values())
                .filter(OrganizationCsvParser.Column::required).map(OrganizationCsvParser.Column::title).toList());
        String optional = String.join(", ", Arrays.stream(OrganizationCsvParser.Column.values())
                .filter(c -> !c.required()).map(OrganizationCsvParser.Column::title).toList());
        String types = String.join(", ", Arrays.stream(OrganizationType.values()).map(Enum::name).toList());

        Div help = new Div();
        help.getStyle().set("color", "var(--lumo-secondary-text-color)");
        help.add(
                new Paragraph("Каждая строка файла — одна организация. Первая строка — заголовок. Кодировка UTF-8, "
                        + "разделитель — запятая или точка с запятой, значения с запятыми заключаются в двойные кавычки."),
                new Paragraph("Обязательные столбцы: " + required + "."),
                new Paragraph("Необязательные столбцы: " + optional + ". Пустое значение означает «не задано»."),
                new Paragraph("Вложенные объекты задаются в той же строке: координаты — столбцами coordinatesX и "
                        + "coordinatesY, адреса — улицей в столбцах officialAddress и postalAddress. Если такие "
                        + "координаты (x, y) или адрес (улица) уже есть в системе или встречались выше в файле, "
                        + "используется существующий объект, дубликаты не создаются."),
                new Paragraph("Полное название (fullName), если задано, должно быть уникальным среди организаций "
                        + "системы и внутри файла (без учёта регистра)."),
                new Paragraph("Допустимые типы: " + types + "."),
                new Paragraph("Импорт выполняется целиком: если в файле найдена хотя бы одна ошибка, "
                        + "ни одна организация не будет добавлена. Максимум — " + OrganizationCsvParser.MAX_ROWS
                        + " организаций и " + ImportService.MAX_FILE_BYTES / (1024 * 1024) + " МБ."),
                new Paragraph("Пример файла:"));

        Div example = new Div(EXAMPLE.strip());
        example.getStyle()
                .set("white-space", "pre")
                .set("overflow-x", "auto")
                .set("font-family", "monospace")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("padding", "var(--lumo-space-s)")
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-radius", "var(--lumo-border-radius-m)");
        help.add(example);
        return help;
    }

    /** Вызывается после завершения загрузки файла на сервер. */
    private void onUploaded(String name, byte[] data) {
        content = data;
        fileName = name;
        getUI().ifPresent(ui -> ui.access(() -> {
            selected.setText("Файл загружен на сервер: " + name + " (" + data.length + " байт)");
            run.setEnabled(true);
            result.removeAll();
        }));
    }

    private void forgetFile() {
        content = null;
        fileName = null;
        selected.setText("Файл не выбран");
        run.setEnabled(false);
    }

    private void runImport() {
        byte[] data = content;
        String user = UserSession.currentUser().orElse(null);
        if (data == null) {
            Notifications.error("Сначала выберите файл");
            return;
        }
        if (user == null) {
            Notifications.error("Войдите в систему, чтобы выполнить импорт");
            return;
        }
        run.setEnabled(false);
        try {
            ImportResult outcome = importService.importOrganizations(user, fileName, data);
            show(outcome);
            if (outcome.success()) {
                forgetFile();
                selected.setText("Файл обработан. Чтобы импортировать другой файл, удалите этот из списка и загрузите новый.");
            } else {
                run.setEnabled(true);
            }
        } catch (RuntimeException ex) {
            run.setEnabled(true);
            UiErrors.show(ex);
        }
    }

    private void show(ImportResult outcome) {
        result.removeAll();
        String id = outcome.operation() == null ? "" : "#" + outcome.operation().getId() + ": ";
        if (outcome.success()) {
            Span ok = new Span("Импорт выполнен. Операция " + id + "добавлено организаций: "
                    + outcome.operation().getAddedCount() + ".");
            ok.getStyle().set("font-weight", "600").set("color", "var(--lumo-success-text-color)");
            result.add(ok);
            Notifications.success("Импорт выполнен, добавлено организаций: " + outcome.operation().getAddedCount());
            return;
        }
        Span failed = new Span("Импорт не выполнен, ни одна организация не добавлена. "
                + (outcome.operation() == null ? "" : "Операция #" + outcome.operation().getId() + ". ")
                + "Исправьте ошибки и загрузите файл заново:");
        failed.getStyle().set("font-weight", "600").set("color", "var(--lumo-error-text-color)");

        Div list = new Div();
        list.getStyle()
                .set("max-height", "320px")
                .set("overflow", "auto")
                .set("margin-top", "var(--lumo-space-s)")
                .set("padding", "var(--lumo-space-s)")
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "var(--lumo-border-radius-m)");
        outcome.errors().forEach(message -> list.add(new Div(message)));
        result.add(failed, list);
        Notifications.error("Импорт не выполнен: найдены ошибки в файле");
    }
}
