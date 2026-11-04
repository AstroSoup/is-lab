package ru.astrosoup.islab.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import ru.astrosoup.islab.entities.ImportOperation;
import ru.astrosoup.islab.exceptions.BusinessException;
import ru.astrosoup.islab.exceptions.EntityValidationException;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class ImportService {
    public static final int MAX_FILE_BYTES = 5 * 1024 * 1024;
    private static final int MAX_FILE_NAME = 255;
    private static final Logger LOG = Logger.getLogger(ImportService.class.getName());

    @Inject
    ImportExecutor executor;
    @Inject
    ImportHistoryService history;

    public ImportResult importOrganizations(String userName, String fileName, byte[] content) {
        if (userName == null || userName.isBlank()) {
            throw new IllegalArgumentException("Не указан пользователь, запустивший импорт");
        }
        String name = normalizeFileName(fileName);
        try {
            if (content != null && content.length > MAX_FILE_BYTES) {
                throw new BusinessException("Файл слишком большой: допускается не более "
                        + MAX_FILE_BYTES / (1024 * 1024) + " МБ");
            }
            ImportOperation operation = executor.execute(userName, name, content);
            return new ImportResult(operation, List.of());
        } catch (RuntimeException e) {
            List<String> problems = describe(e);
            ImportOperation failed = null;
            try {
                failed = history.recordFailure(userName, name, String.join("\n", problems));
            } catch (RuntimeException historyError) {
                LOG.log(Level.SEVERE, "Не удалось записать неуспешный импорт в историю", historyError);
            }
            return new ImportResult(failed, problems);
        }
    }

    private static List<String> describe(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause() == c ? null : c.getCause()) {
            if (c instanceof EntityValidationException validation) {
                return validation.getViolations();
            }
            if (c instanceof BusinessException) {
                return List.of(c.getMessage());
            }
        }
        LOG.log(Level.SEVERE, "Непредвиденная ошибка импорта", t);
        Throwable root = t;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return List.of("Непредвиденная ошибка при импорте: "
                + (root.getMessage() == null ? root.getClass().getSimpleName() : root.getMessage()));
    }

    private static String normalizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "(без имени)";
        }
        String trimmed = fileName.trim();
        return trimmed.length() <= MAX_FILE_NAME ? trimmed : trimmed.substring(0, MAX_FILE_NAME);
    }
}
