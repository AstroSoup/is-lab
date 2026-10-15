package ru.astrosoup.islab.exceptions;

import java.util.List;

public class EntityValidationException extends BusinessException {
    private final List<String> violations;

    public EntityValidationException(List<String> violations) {
        super(String.join("; ", violations));
        this.violations = List.copyOf(violations);
    }

    public List<String> getViolations() {
        return violations;
    }
}
