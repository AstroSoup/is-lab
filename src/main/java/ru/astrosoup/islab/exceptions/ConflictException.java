package ru.astrosoup.islab.exceptions;

/** Конфликт с параллельной операцией другого пользователя (устаревшая версия объекта, сбой сериализации и т. п.). */
public class ConflictException extends BusinessException {
    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
