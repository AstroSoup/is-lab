package ru.astrosoup.islab.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ru.astrosoup.islab.entities.Coordinates;
import ru.astrosoup.islab.events.DataChangedEvent;
import ru.astrosoup.islab.events.EntityKind;
import ru.astrosoup.islab.exceptions.BusinessException;
import ru.astrosoup.islab.repositories.CoordinatesRepository;
import ru.astrosoup.islab.repositories.OrganizationRepository;

import java.util.List;

@ApplicationScoped
@Transactional
public class CoordinatesService {

    @Inject
    CoordinatesRepository repository;
    @Inject
    OrganizationRepository organizations;
    @Inject
    BeanValidator validator;
    @Inject
    Event<DataChangedEvent> events;

    public List<Coordinates> findAll() {
        return repository.findAll();
    }

    public Coordinates get(long id) {
        Coordinates c = repository.find(id);
        if (c == null) {
            throw new BusinessException("Координаты #" + id + " не найдены (возможно, удалены другим пользователем)");
        }
        return c;
    }

    public Coordinates create(Coordinates draft) {
        draft.setId(0);
        validator.check(draft);
        repository.persist(draft);
        PersistenceFailures.flush(repository::flush, "Конфликт при сохранении координат");
        events.fire(new DataChangedEvent(EntityKind.COORDINATES));
        return draft;
    }

    public Coordinates update(Coordinates draft) {
        Coordinates managed = get(draft.getId());
        managed.setX(draft.getX());
        managed.setY(draft.getY());
        validator.check(managed);
        PersistenceFailures.flush(repository::flush, "Конфликт при сохранении координат");
        events.fire(new DataChangedEvent(EntityKind.COORDINATES));
        return managed;
    }

    public void delete(long id) {
        Coordinates c = get(id);
        long used = organizations.countByCoordinates(c);
        if (used > 0) {
            throw new BusinessException("Невозможно удалить координаты #" + id
                    + ": они используются организациями (" + used + " шт.). Сначала измените или удалите эти организации.");
        }
        repository.remove(c);
        PersistenceFailures.flush(repository::flush,
                "Невозможно удалить координаты #" + id + ": на них уже ссылается другая организация");
        events.fire(new DataChangedEvent(EntityKind.COORDINATES));
    }
}
