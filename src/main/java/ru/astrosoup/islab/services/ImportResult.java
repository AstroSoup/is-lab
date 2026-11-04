package ru.astrosoup.islab.services;

import ru.astrosoup.islab.entities.ImportOperation;
import ru.astrosoup.islab.entities.ImportStatus;

import java.util.List;


public record ImportResult(ImportOperation operation, List<String> errors) {

    public boolean success() {
        return operation != null && operation.getStatus() == ImportStatus.SUCCESS;
    }
}
