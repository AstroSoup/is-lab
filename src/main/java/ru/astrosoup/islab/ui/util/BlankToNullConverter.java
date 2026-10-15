package ru.astrosoup.islab.ui.util;

import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;

/** Пустая/пробельная строка → null (для необязательных строковых полей). */
public class BlankToNullConverter implements Converter<String, String> {
    @Override
    public Result<String> convertToModel(String value, ValueContext context) {
        return Result.ok(value == null || value.isBlank() ? null : value.trim());
    }

    @Override
    public String convertToPresentation(String value, ValueContext context) {
        return value == null ? "" : value;
    }
}
