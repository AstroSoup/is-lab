package ru.astrosoup.islab.imports;

import java.util.ArrayList;
import java.util.List;


final class CsvParser {

    record Record(int line, List<String> fields) {
        boolean isBlank() {
            return fields.stream().allMatch(String::isBlank);
        }
    }

    static final class CsvFormatException extends RuntimeException {
        CsvFormatException(String message) {
            super(message);
        }
    }

    private CsvParser() {
    }

    static List<Record> parse(String text, char delimiter) {
        List<Record> records = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        boolean closedQuote = false;
        boolean fieldStart = true;
        int line = 1;
        int recordLine = 1;
        int n = text.length();

        for (int i = 0; i < n; i++) {
            char ch = text.charAt(i);
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < n && text.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                        closedQuote = true;
                    }
                } else {
                    if (ch == '\n') {
                        line++;
                    }
                    cur.append(ch);
                }
                continue;
            }
            if (ch == delimiter) {
                fields.add(cur.toString());
                cur.setLength(0);
                closedQuote = false;
                fieldStart = true;
            } else if (ch == '\n' || ch == '\r') {
                if (ch == '\r' && i + 1 < n && text.charAt(i + 1) == '\n') {
                    i++;
                }
                fields.add(cur.toString());
                cur.setLength(0);
                records.add(new Record(recordLine, fields));
                fields = new ArrayList<>();
                closedQuote = false;
                fieldStart = true;
                line++;
                recordLine = line;
            } else if (closedQuote) {
                throw new CsvFormatException("строка " + line
                        + ": после закрывающей кавычки допускается только разделитель или конец строки");
            } else if (ch == '"' && fieldStart) {
                inQuotes = true;
                fieldStart = false;
            } else {
                cur.append(ch);
                fieldStart = false;
            }
        }

        if (inQuotes) {
            throw new CsvFormatException("строка " + recordLine + ": не закрыта кавычка");
        }
        if (!fields.isEmpty() || cur.length() > 0 || closedQuote) {
            fields.add(cur.toString());
            records.add(new Record(recordLine, fields));
        }
        return records;
    }
}
