package ru.astrosoup.islab.imports;

import ru.astrosoup.islab.entities.OrganizationType;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;


public final class OrganizationCsvParser {
    public static final int MAX_ROWS = 10_000;

    public enum Column {
        NAME("name", true),
        FULL_NAME("fullName", false),
        COORDINATES_X("coordinatesX", true),
        COORDINATES_Y("coordinatesY", true),
        ANNUAL_TURNOVER("annualTurnover", true),
        EMPLOYEES_COUNT("employeesCount", true),
        RATING("rating", true),
        TYPE("type", false),
        OFFICIAL_ADDRESS("officialAddress", false),
        POSTAL_ADDRESS("postalAddress", false);

        private final String title;
        private final boolean required;

        Column(String title, boolean required) {
            this.title = title;
            this.required = required;
        }

        public String title() {
            return title;
        }

        public boolean required() {
            return required;
        }
    }

    public record ParseResult(List<ImportRow> rows, ErrorLog errors) {
    }

    private static final Pattern INTEGER = Pattern.compile("[+-]?\\d+");
    private static final Pattern DECIMAL = Pattern.compile("[+-]?(\\d+(\\.\\d*)?|\\.\\d+)([eE][+-]?\\d+)?");

    private OrganizationCsvParser() {
    }

    public static ParseResult parse(byte[] content) {
        ErrorLog errors = new ErrorLog();
        List<ImportRow> rows = new ArrayList<>();
        ParseResult result = new ParseResult(rows, errors);

        if (content == null || content.length == 0) {
            errors.add("Файл пуст");
            return result;
        }
        String text;
        try {
            text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(content)).toString();
        } catch (CharacterCodingException e) {
            errors.add("Файл должен быть в кодировке UTF-8");
            return result;
        }
        if (text.startsWith("﻿")) {
            text = text.substring(1);
        }

        List<CsvParser.Record> records;
        try {
            records = CsvParser.parse(text, detectDelimiter(text));
        } catch (CsvParser.CsvFormatException e) {
            errors.add(e.getMessage());
            return result;
        }
        records.removeIf(CsvParser.Record::isBlank);
        if (records.isEmpty()) {
            errors.add("Файл не содержит данных");
            return result;
        }

        Map<Column, Integer> index = readHeader(records.get(0), errors);
        if (index == null) {
            return result;
        }
        int width = records.get(0).fields().size();
        if (records.size() == 1) {
            errors.add("В файле есть только строка заголовка — нет ни одной организации");
            return result;
        }
        if (records.size() - 1 > MAX_ROWS) {
            errors.add("В файле слишком много строк: допускается не более " + MAX_ROWS);
            return result;
        }

        for (CsvParser.Record record : records.subList(1, records.size())) {
            if (record.fields().size() != width) {
                errors.add("строка " + record.line() + ": ожидалось значений: " + width
                        + ", найдено: " + record.fields().size());
                continue;
            }
            ImportRow row = parseRow(record, index, errors);
            if (row != null) {
                rows.add(row);
            }
        }
        return result;
    }

    private static char detectDelimiter(String text) {
        int end = text.indexOf('\n');
        String header = end < 0 ? text : text.substring(0, end);
        long commas = header.chars().filter(c -> c == ',').count();
        long semicolons = header.chars().filter(c -> c == ';').count();
        return semicolons > commas ? ';' : ',';
    }

    private static Map<Column, Integer> readHeader(CsvParser.Record header, ErrorLog errors) {
        Map<String, Column> byName = new HashMap<>();
        for (Column c : Column.values()) {
            byName.put(c.title.toLowerCase(Locale.ROOT), c);
        }
        Map<Column, Integer> index = new EnumMap<>(Column.class);
        List<String> fields = header.fields();
        for (int i = 0; i < fields.size(); i++) {
            String title = fields.get(i).trim();
            Column column = byName.get(title.toLowerCase(Locale.ROOT));
            if (column == null) {
                errors.add("Заголовок: неизвестный столбец «" + title + "». Допустимые столбцы: " + allTitles());
            } else if (index.put(column, i) != null) {
                errors.add("Заголовок: столбец «" + column.title + "» указан более одного раза");
            }
        }
        List<String> missing = new ArrayList<>();
        for (Column c : Column.values()) {
            if (c.required && !index.containsKey(c)) {
                missing.add(c.title);
            }
        }
        if (!missing.isEmpty()) {
            errors.add("Заголовок: отсутствуют обязательные столбцы: " + String.join(", ", missing));
        }
        return errors.isEmpty() ? index : null;
    }

    private static String allTitles() {
        return String.join(", ", Arrays.stream(Column.values()).map(Column::title).toList());
    }

    private static ImportRow parseRow(CsvParser.Record record, Map<Column, Integer> index, ErrorLog errors) {
        int before = errors.total();
        int line = record.line();
        List<String> f = record.fields();

        String name = text(f, index, Column.NAME);
        String fullName = text(f, index, Column.FULL_NAME);
        Double x = decimal(f, index, Column.COORDINATES_X, line, errors);
        Integer y = integer(f, index, Column.COORDINATES_Y, line, errors);
        Long turnover = longValue(f, index, Column.ANNUAL_TURNOVER, line, errors);
        Integer employees = integer(f, index, Column.EMPLOYEES_COUNT, line, errors);
        Integer rating = integer(f, index, Column.RATING, line, errors);
        OrganizationType type = type(f, index, line, errors);
        String official = text(f, index, Column.OFFICIAL_ADDRESS);
        String postal = text(f, index, Column.POSTAL_ADDRESS);

        if (errors.total() > before) {
            return null;
        }
        return new ImportRow(line, name, fullName, x, y, turnover, employees, rating, type, official, postal);
    }

    private static String text(List<String> fields, Map<Column, Integer> index, Column column) {
        Integer i = index.get(column);
        if (i == null) return null;
        String value = fields.get(i).trim();
        return value.isEmpty() ? null : value;
    }

    private static Double decimal(List<String> fields, Map<Column, Integer> index, Column column,
                                  int line, ErrorLog errors) {
        String value = text(fields, index, column);
        if (value == null) {
            errors.add(required(line, column));
            return null;
        }
        if (!DECIMAL.matcher(value).matches()) {
            errors.add(invalid(line, column, "числом (разделитель дробной части — точка)", value));
            return null;
        }
        double d = Double.parseDouble(value);
        if (!Double.isFinite(d)) {
            errors.add("строка " + line + ": значение поля «" + column.title + "» выходит за допустимые пределы");
            return null;
        }
        return d;
    }

    private static Integer integer(List<String> fields, Map<Column, Integer> index, Column column,
                                   int line, ErrorLog errors) {
        String value = text(fields, index, column);
        if (value == null) {
            errors.add(required(line, column));
            return null;
        }
        if (!INTEGER.matcher(value).matches()) {
            errors.add(invalid(line, column, "целым числом", value));
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            errors.add("строка " + line + ": значение поля «" + column.title + "» выходит за допустимые пределы");
            return null;
        }
    }

    private static Long longValue(List<String> fields, Map<Column, Integer> index, Column column,
                                  int line, ErrorLog errors) {
        String value = text(fields, index, column);
        if (value == null) {
            errors.add(required(line, column));
            return null;
        }
        if (!INTEGER.matcher(value).matches()) {
            errors.add(invalid(line, column, "целым числом", value));
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            errors.add("строка " + line + ": значение поля «" + column.title + "» выходит за допустимые пределы");
            return null;
        }
    }

    private static OrganizationType type(List<String> fields, Map<Column, Integer> index, int line, ErrorLog errors) {
        String value = text(fields, index, Column.TYPE);
        if (value == null) {
            return null;
        }
        try {
            return OrganizationType.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            errors.add("строка " + line + ": неизвестный тип организации «" + value + "». Допустимые значения: "
                    + String.join(", ", Arrays.stream(OrganizationType.values()).map(Enum::name).toList()));
            return null;
        }
    }

    private static String required(int line, Column column) {
        return "строка " + line + ": поле «" + column.title + "» обязательно";
    }

    private static String invalid(int line, Column column, String expected, String actual) {
        return "строка " + line + ": поле «" + column.title + "» должно быть " + expected + " (получено «" + actual + "»)";
    }
}
