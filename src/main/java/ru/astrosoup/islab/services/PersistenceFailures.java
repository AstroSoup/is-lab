package ru.astrosoup.islab.services;

import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import ru.astrosoup.islab.exceptions.BusinessException;

final class PersistenceFailures {
    private PersistenceFailures() {
    }

    static void flush(Runnable flush, String conflictMessage) {
        try {
            flush.run();
        } catch (OptimisticLockException e) {
            throw new BusinessException(conflictMessage, e);
        } catch (PersistenceException e) {
            throw new BusinessException("Ошибка сохранения в базе данных: " + rootMessage(e), e);
        }
    }

    static String rootMessage(Throwable t) {
        Throwable root = t;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String msg = root.getMessage();
        return msg == null ? root.getClass().getSimpleName() : msg.lines().findFirst().orElse(msg);
    }
}
