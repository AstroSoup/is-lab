package ru.astrosoup.islab.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import ru.astrosoup.islab.exceptions.EntityValidationException;

import java.util.List;
import java.util.Set;

@ApplicationScoped
public class BeanValidator {

    @Inject
    Validator validator;

    public void check(Object bean) {
        List<String> messages = messages(bean);
        if (!messages.isEmpty()) {
            throw new EntityValidationException(messages);
        }
    }

    /** Сообщения обо всех нарушенных ограничениях объекта; пустой список, если объект корректен. */
    public List<String> messages(Object bean) {
        Set<ConstraintViolation<Object>> violations = validator.validate(bean);
        return violations.stream()
                .map(ConstraintViolation::getMessage)
                .distinct()
                .sorted()
                .toList();
    }
}
