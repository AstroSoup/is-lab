package ru.astrosoup.islab.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ru.astrosoup.islab.entities.Address;
import ru.astrosoup.islab.events.DataChangedEvent;
import ru.astrosoup.islab.events.EntityKind;
import ru.astrosoup.islab.exceptions.BusinessException;
import ru.astrosoup.islab.repositories.AddressRepository;
import ru.astrosoup.islab.repositories.OrganizationRepository;

import java.util.List;

@ApplicationScoped
@Transactional
public class AddressService {

    @Inject
    AddressRepository repository;
    @Inject
    OrganizationRepository organizations;
    @Inject
    BeanValidator validator;
    @Inject
    Event<DataChangedEvent> events;

    public List<Address> findAll() {
        return repository.findAll();
    }

    public Address get(long id) {
        Address a = repository.find(id);
        if (a == null) {
            throw new BusinessException("Адрес #" + id + " не найден (возможно, удалён другим пользователем)");
        }
        return a;
    }

    public Address create(Address draft) {
        draft.setId(0);
        draft.setStreet(normalize(draft.getStreet()));
        validator.check(draft);
        repository.persist(draft);
        PersistenceFailures.flush(repository::flush, "Конфликт при сохранении адреса");
        events.fire(new DataChangedEvent(EntityKind.ADDRESS));
        return draft;
    }

    public Address update(Address draft) {
        Address managed = get(draft.getId());
        managed.setStreet(normalize(draft.getStreet()));
        validator.check(managed);
        PersistenceFailures.flush(repository::flush, "Конфликт при сохранении адреса");
        events.fire(new DataChangedEvent(EntityKind.ADDRESS));
        return managed;
    }

    public void delete(long id) {
        Address a = get(id);
        long used = organizations.countByAddress(a);
        if (used > 0) {
            throw new BusinessException("Невозможно удалить адрес #" + id
                    + ": он используется организациями (" + used + " шт.). Сначала измените или удалите эти организации.");
        }
        repository.remove(a);
        PersistenceFailures.flush(repository::flush,
                "Невозможно удалить адрес #" + id + ": на него уже ссылается другая организация");
        events.fire(new DataChangedEvent(EntityKind.ADDRESS));
    }

    static String normalize(String street) {
        return street == null || street.isBlank() ? null : street.trim();
    }
}
