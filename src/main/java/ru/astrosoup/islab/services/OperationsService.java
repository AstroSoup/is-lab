package ru.astrosoup.islab.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ru.astrosoup.islab.entities.Address;
import ru.astrosoup.islab.entities.Organization;
import ru.astrosoup.islab.entities.OrganizationType;
import ru.astrosoup.islab.events.DataChangedEvent;
import ru.astrosoup.islab.events.EntityKind;
import ru.astrosoup.islab.exceptions.BusinessException;
import ru.astrosoup.islab.repositories.AddressRepository;
import ru.astrosoup.islab.repositories.OrganizationRepository;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;

@ApplicationScoped
@Transactional
public class OperationsService {
    private static final String CONFLICT = "Данные были изменены другим пользователем. Повторите операцию.";

    @Inject
    OrganizationRepository organizations;
    @Inject
    AddressRepository addresses;
    @Inject
    BeanValidator validator;
    @Inject
    Event<DataChangedEvent> events;

    public OptionalDouble averageRating() {
        List<Integer> ratings = organizations.findAllRatings();
        return ratings.stream().mapToInt(Integer::intValue).average();
    }

    public long countByPostalAddress(String street) {
        String wanted = AddressService.normalize(street);
        if (wanted == null) {
            throw new BusinessException("Укажите улицу почтового адреса");
        }
        return organizations.findAll().stream()
                .map(Organization::getPostalAddress)
                .filter(a -> a != null && Objects.equals(a.getStreet(), wanted))
                .count();
    }

    public long countWithoutPostalAddress() {
        return organizations.findAll().stream().filter(o -> o.getPostalAddress() == null).count();
    }

    public long countByTypeGreaterThan(OrganizationType threshold) {
        if (threshold == null) {
            throw new BusinessException("Выберите тип организации");
        }
        return organizations.findAllTypes().stream()
                .filter(Objects::nonNull)
                .filter(t -> t.compareTo(threshold) > 0)
                .count();
    }

    public Organization merge(long firstId, long secondId, String newName, String newStreet) {
        if (firstId == secondId) {
            throw new BusinessException("Для объединения выберите две разные организации");
        }
        if (newName == null || newName.isBlank()) {
            throw new BusinessException("Укажите название новой организации");
        }
        String street = AddressService.normalize(newStreet);
        if (street == null) {
            throw new BusinessException("Укажите адрес новой организации");
        }

        Organization[] pair = lockBoth(firstId, secondId);
        Organization first = pair[0];
        Organization second = pair[1];

        Address address = addresses.findByStreet(street);
        boolean addressCreated = false;
        if (address == null) {
            address = new Address(street);
            addresses.persist(address);
            addressCreated = true;
        }

        Organization merged = new Organization();
        merged.setName(newName.trim());
        merged.setCreationDate(new Date());
        merged.setCoordinates(first.getCoordinates());
        merged.setOfficialAddress(address);
        try {
            merged.setEmployeesCount(Math.addExact(first.getEmployeesCount(), second.getEmployeesCount()));
            merged.setAnnualTurnover(Math.addExact(first.getAnnualTurnover(), second.getAnnualTurnover()));
        } catch (ArithmeticException e) {
            throw new BusinessException("Невозможно объединить: суммарное значение сотрудников или оборота слишком велико");
        }
        merged.setRating(Math.max(1, (int) Math.ceil((first.getRating() + (double) second.getRating()) / 2)));
        merged.setType(first.getType() == second.getType() ? first.getType() : null);

        validator.check(merged);
        organizations.remove(first);
        organizations.remove(second);
        organizations.persist(merged);
        PersistenceFailures.flush(organizations::flush, CONFLICT);

        events.fire(new DataChangedEvent(EntityKind.ORGANIZATION));
        if (addressCreated) {
            events.fire(new DataChangedEvent(EntityKind.ADDRESS));
        }
        return merged;
    }

    public Organization absorb(long acquirerId, long targetId) {
        if (acquirerId == targetId) {
            throw new BusinessException("Организация не может поглотить саму себя");
        }
        Organization[] pair = lockBoth(acquirerId, targetId);
        Organization acquirer = pair[0];
        Organization target = pair[1];

        try {
            acquirer.setEmployeesCount(Math.addExact(acquirer.getEmployeesCount(), target.getEmployeesCount()));
            acquirer.setAnnualTurnover(Math.addExact(acquirer.getAnnualTurnover(), target.getAnnualTurnover()));
        } catch (ArithmeticException e) {
            throw new BusinessException("Невозможно поглотить: суммарное значение сотрудников или оборота слишком велико");
        }
        validator.check(acquirer);
        organizations.remove(target);
        PersistenceFailures.flush(organizations::flush, CONFLICT);

        events.fire(new DataChangedEvent(EntityKind.ORGANIZATION));
        return acquirer;
    }

    private Organization[] lockBoth(long firstId, long secondId) {
        Organization first;
        Organization second;
        if (firstId < secondId) {
            first = require(organizations.findForUpdate(firstId), firstId);
            second = require(organizations.findForUpdate(secondId), secondId);
        } else {
            second = require(organizations.findForUpdate(secondId), secondId);
            first = require(organizations.findForUpdate(firstId), firstId);
        }
        return new Organization[]{first, second};
    }

    private static Organization require(Organization o, long id) {
        if (o == null) {
            throw new BusinessException("Организация #" + id + " не найдена (возможно, удалена другим пользователем)");
        }
        return o;
    }
}
