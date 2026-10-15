package ru.astrosoup.islab.events;

import java.io.Serializable;

/** Событие «данные изменились» (создание / изменение / удаление объекта). */
public record DataChangedEvent(EntityKind kind) implements Serializable {
}
