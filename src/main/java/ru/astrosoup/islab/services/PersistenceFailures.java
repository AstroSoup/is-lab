package ru.astrosoup.islab.services;

import jakarta.persistence.PersistenceException;
import ru.astrosoup.islab.exceptions.BusinessException;
import ru.astrosoup.islab.exceptions.ConflictException;
import ru.astrosoup.islab.tx.SerializationFailures;

final class PersistenceFailures {
    private PersistenceFailures() {
    }

    static void flush(Runnable flush, String conflictMessage) {
        try {
            flush.run();
        } catch (RuntimeException e) {
            if (SerializationFailures.isSerializationFailure(e)) {
                throw new ConflictException(SerializationFailures.MESSAGE, e);
            }
            if (SerializationFailures.isOptimisticLockFailure(e)) {
                throw new ConflictException(conflictMessage, e);
            }
            if (e instanceof PersistenceException) {
                throw new BusinessException("Ошибка сохранения в базе данных: " + rootMessage(e), e);
            }
            throw e;
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
