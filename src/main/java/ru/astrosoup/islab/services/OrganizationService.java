package ru.astrosoup.islab.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ru.astrosoup.islab.entities.Address;
import ru.astrosoup.islab.entities.Coordinates;
import ru.astrosoup.islab.entities.Organization;
import ru.astrosoup.islab.events.DataChangedEvent;
import ru.astrosoup.islab.events.EntityKind;
import ru.astrosoup.islab.exceptions.BusinessException;
import ru.astrosoup.islab.exceptions.EntityValidationException;
import ru.astrosoup.islab.repositories.*;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
@Transactional
public class OrganizationService {
    private static final String CONFLICT =
            "Организация была изменена другим пользователем. Закройте окно и откройте её заново.";

    @Inject
    OrganizationRepository repository;
    @Inject
    CoordinatesRepository coordinatesRepository;
    @Inject
    AddressRepository addressRepository;
    @Inject
    BeanValidator validator;
    @Inject
    Event<DataChangedEvent> events;

    public Optional<Organization> find(long id) {
        return Optional.ofNullable(repository.find(id));
    }

    public Organization get(long id) {
        return find(id).orElseThrow(() -> new BusinessException(
                "Организация с id=" + id + " не найдена (возможно, она удалена другим пользователем)"));
    }

    public Organization create(Organization draft) {
        draft.setId(0);
        draft.setVersion(0);
        draft.setCreationDate(new Date()); // генерируется автоматически
        normalize(draft);
        resolveReferences(draft);
        validator.check(draft);
        repository.persist(draft);
        PersistenceFailures.flush(repository::flush, CONFLICT);
        events.fire(new DataChangedEvent(EntityKind.ORGANIZATION));
        return draft;
    }

    public Organization update(Organization draft) {
        Organization managed = repository.find(draft.getId());
        if (managed == null) {
            throw new BusinessException("Организация с id=" + draft.getId()
                    + " не найдена (возможно, она удалена другим пользователем)");
        }
        if (managed.getVersion() != draft.getVersion()) {
            throw new BusinessException(CONFLICT);
        }
        normalize(draft);
        resolveReferences(draft);
        managed.copyEditableFieldsFrom(draft);
        validator.check(managed);
        PersistenceFailures.flush(repository::flush, CONFLICT);
        events.fire(new DataChangedEvent(EntityKind.ORGANIZATION));
        return managed;
    }

    public void delete(long id) {
        Organization organization = get(id);
        repository.remove(organization);
        PersistenceFailures.flush(repository::flush,
                "Невозможно удалить организацию #" + id + ": с ней связаны другие объекты");
        events.fire(new DataChangedEvent(EntityKind.ORGANIZATION));
    }


    public List<Organization> findPage(OrganizationFilter filter, List<SortSpec> sort, int offset, int limit) {
        return repository.findPage(filter, sort, offset, limit);
    }

    public long count(OrganizationFilter filter) {
        return repository.count(filter);
    }

    private void normalize(Organization o) {
        if (o.getName() != null) {
            o.setName(o.getName().trim());
        }
        if (o.getFullName() != null) {
            String t = o.getFullName().trim();
            o.setFullName(t.isEmpty() ? null : t);
        }
    }

    private void resolveReferences(Organization o) {
        if (o.getCoordinates() == null) {
            throw new EntityValidationException(List.of("Координаты обязательны"));
        }
        Coordinates c = coordinatesRepository.find(o.getCoordinates().getId());
        if (c == null) {
            throw new BusinessException("Выбранные координаты #" + o.getCoordinates().getId() + " больше не существуют");
        }
        o.setCoordinates(c);
        o.setOfficialAddress(resolveAddress(o.getOfficialAddress()));
        o.setPostalAddress(resolveAddress(o.getPostalAddress()));
    }

    private Address resolveAddress(Address a) {
        if (a == null) return null;
        Address found = addressRepository.find(a.getId());
        if (found == null) {
            throw new BusinessException("Выбранный адрес #" + a.getId() + " больше не существует");
        }
        return found;
    }
}
