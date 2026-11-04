package ru.astrosoup.islab.imports;

import java.util.ArrayList;
import java.util.List;

public final class ErrorLog {
    public static final int LIMIT = 100;

    private final List<String> items = new ArrayList<>();
    private int total;

    public void add(String message) {
        total++;
        if (items.size() < LIMIT) {
            items.add(message);
        }
    }

    public boolean isEmpty() {
        return total == 0;
    }

    public int total() {
        return total;
    }

    public List<String> toList() {
        List<String> result = new ArrayList<>(items);
        if (total > items.size()) {
            result.add("… и ещё ошибок: " + (total - items.size()) + " (показаны первые " + items.size() + ")");
        }
        return result;
    }
}
