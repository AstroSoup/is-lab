package ru.astrosoup.islab.exceptions;

/** Объект не найден (в том числе потому, что его уже удалил другой пользователь). */
public class ObjectNotFoundException extends BusinessException {
    public ObjectNotFoundException(String message) {
        super(message);
    }
}
