package ru.astrosoup.islab.entities;

public enum ImportStatus {
    SUCCESS("Успешно"),
    FAILED("Ошибка");

    private final String label;

    ImportStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
