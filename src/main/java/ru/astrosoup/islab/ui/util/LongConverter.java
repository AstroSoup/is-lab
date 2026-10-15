package ru.astrosoup.islab.ui.util;

import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;

/** Строка ↔ long (без разделителей групп, с понятным сообщением об ошибке). */
public class LongConverter implements Converter<String, Long> {
    @Override
    public Result<Long> convertToModel(String value, ValueContext context) {
        if (value == null || value.isBlank()) {
            return Result.error("Поле обязательно для заполнения");
        }
        try {
            return Result.ok(Long.parseLong(value.trim()));
        } catch (NumberFormatException e) {
            return Result.error("Введите целое число не более 9223372036854775807");
        }
    }

    @Override
    public String convertToPresentation(Long value, ValueContext context) {
        return value == null ? "" : value.toString();
    }
}
