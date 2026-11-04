package ru.astrosoup.islab.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ru.astrosoup.islab.entities.Address;
import ru.astrosoup.islab.entities.Coordinates;
import ru.astrosoup.islab.entities.ImportOperation;
import ru.astrosoup.islab.entities.Organization;
import ru.astrosoup.islab.events.DataChangedEvent;
import ru.astrosoup.islab.events.EntityKind;
import ru.astrosoup.islab.exceptions.EntityValidationException;
import ru.astrosoup.islab.imports.ErrorLog;
import ru.astrosoup.islab.imports.ImportRow;
import ru.astrosoup.islab.imports.OrganizationCsvParser;
import ru.astrosoup.islab.repositories.AddressRepository;
import ru.astrosoup.islab.repositories.CoordinatesRepository;
import ru.astrosoup.islab.repositories.ImportOperationRepository;
import ru.astrosoup.islab.repositories.OrganizationRepository;
import ru.astrosoup.islab.tx.SerializableIsolation;

import java.util.*;

@ApplicationScoped
@Transactional
public class ImportExecutor {
    private static final String CONFLICT = "Данные были изменены другим пользователем. Повторите импорт.";

    @Inject
    OrganizationRepository organizations;
    @Inject
    CoordinatesRepository coordinates;
    @Inject
    AddressRepository addresses;
    @Inject
    ImportOperationRepository history;
    @Inject
    BeanValidator validator;
    @Inject
    UniquenessRules uniqueness;
    @Inject
    Event<DataChangedEvent> events;

    private record CoordinatesKey(double x, int y) {
    }


    @SerializableIsolation
    public ImportOperation execute(String userName, String fileName, byte[] content) {
        OrganizationCsvParser.ParseResult parsed = OrganizationCsvParser.parse(content);
        ErrorLog errors = parsed.errors();
        if (parsed.rows().isEmpty()) {
            throw new EntityValidationException(errors.toList());
        }

        Set<String> takenFullNames = new HashSet<>();
        for (String fullName : organizations.findAllFullNames()) {
            String key = UniquenessRules.fullNameKey(fullName);
            if (key != null) {
                takenFullNames.add(key);
            }
        }
        Map<String, Integer> fullNameLines = new HashMap<>();

        Map<String, Address> addressByStreet = new HashMap<>();
        for (Address a : addresses.findAll()) {
            if (a.getStreet() != null) {
                addressByStreet.putIfAbsent(a.getStreet(), a);
            }
        }
        Map<CoordinatesKey, Coordinates> coordinatesByKey = new HashMap<>();
        for (Coordinates c : coordinates.findAll()) {
            coordinatesByKey.putIfAbsent(new CoordinatesKey(normalizeZero(c.getX()), c.getY()), c);
        }

        List<Address> newAddresses = new ArrayList<>();
        List<Coordinates> newCoordinates = new ArrayList<>();
        List<Organization> newOrganizations = new ArrayList<>();

        for (ImportRow row : parsed.rows()) {
            int errorsBefore = errors.total();

            Coordinates c = new Coordinates();
            c.setX(normalizeZero(row.x()));
            c.setY(row.y());

            Organization o = new Organization();
            o.setName(row.name());
            o.setFullName(row.fullName());
            o.setAnnualTurnover(row.annualTurnover());
            o.setEmployeesCount(row.employeesCount());
            o.setRating(row.rating());
            o.setType(row.type());
            o.setCreationDate(new Date());
            o.setCoordinates(c);

            Address official = row.officialStreet() == null ? null : new Address(row.officialStreet());
            Address postal = row.postalStreet() == null ? null : new Address(row.postalStreet());
            o.setOfficialAddress(official);
            o.setPostalAddress(postal);

            Set<String> violations = new LinkedHashSet<>();
            violations.addAll(validator.messages(c));
            violations.addAll(validator.messages(o));
            if (official != null) violations.addAll(validator.messages(official));
            if (postal != null) violations.addAll(validator.messages(postal));
            for (String message : violations) {
                errors.add(prefix(row) + message);
            }

            String fullNameKey = UniquenessRules.fullNameKey(o.getFullName());
            if (fullNameKey != null) {
                if (takenFullNames.contains(fullNameKey)) {
                    errors.add(prefix(row) + UniquenessRules.fullNameTaken(o.getFullName()));
                } else {
                    Integer firstLine = fullNameLines.putIfAbsent(fullNameKey, row.line());
                    if (firstLine != null) {
                        errors.add(prefix(row) + UniquenessRules.fullNameTaken(o.getFullName())
                                + " (уже указано в строке " + firstLine + ")");
                    }
                }
            }

            if (errors.total() > errorsBefore) {
                continue;
            }

            CoordinatesKey key = new CoordinatesKey(c.getX(), c.getY());
            Coordinates resolved = coordinatesByKey.get(key);
            if (resolved == null) {
                resolved = c;
                coordinatesByKey.put(key, c);
                newCoordinates.add(c);
            }
            o.setCoordinates(resolved);
            o.setOfficialAddress(resolveAddress(official, addressByStreet, newAddresses));
            o.setPostalAddress(resolveAddress(postal, addressByStreet, newAddresses));
            newOrganizations.add(o);
        }

        if (!errors.isEmpty()) {
            throw new EntityValidationException(errors.toList());
        }

        newAddresses.forEach(addresses::persist);
        newCoordinates.forEach(coordinates::persist);
        newOrganizations.forEach(organizations::persist);
        PersistenceFailures.flush(organizations::flush, CONFLICT);

        ImportOperation operation = ImportOperation.success(userName, fileName, newOrganizations.size());
        history.persist(operation);
        PersistenceFailures.flush(history::flush, CONFLICT);

        events.fire(new DataChangedEvent(EntityKind.ORGANIZATION));
        events.fire(new DataChangedEvent(EntityKind.IMPORT_HISTORY));
        if (!newCoordinates.isEmpty()) {
            events.fire(new DataChangedEvent(EntityKind.COORDINATES));
        }
        if (!newAddresses.isEmpty()) {
            events.fire(new DataChangedEvent(EntityKind.ADDRESS));
        }
        return operation;
    }

    private static Address resolveAddress(Address draft, Map<String, Address> byStreet, List<Address> created) {
        if (draft == null) {
            return null;
        }
        Address existing = byStreet.get(draft.getStreet());
        if (existing != null) {
            return existing;
        }
        byStreet.put(draft.getStreet(), draft);
        created.add(draft);
        return draft;
    }

    /** Приводит -0.0 к 0.0, чтобы такие координаты считались одинаковыми. */
    private static double normalizeZero(double value) {
        return value == 0.0 ? 0.0 : value;
    }

    private static String prefix(ImportRow row) {
        return "строка " + row.line() + ": ";
    }
}
