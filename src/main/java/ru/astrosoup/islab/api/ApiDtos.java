package ru.astrosoup.islab.api;

import ru.astrosoup.islab.entities.*;

import java.util.List;

public final class ApiDtos {
    private ApiDtos() {
    }

    public static class Credentials {
        public String name;
        public String password;
    }

    public static class ErrorDto {
        public String error;
        public List<String> violations;
    }

    public static class CoordinatesDto {
        public long id;
        public long version;
        public Double x;
        public int y;

        public static CoordinatesDto of(Coordinates c) {
            CoordinatesDto d = new CoordinatesDto();
            d.id = c.getId();
            d.version = c.getVersion();
            d.x = c.getX();
            d.y = c.getY();
            return d;
        }

        public Coordinates toEntity() {
            Coordinates c = new Coordinates();
            c.setId(id);
            c.setVersion(version);
            c.setX(x);
            c.setY(y);
            return c;
        }
    }

    public static class AddressDto {
        public long id;
        public long version;
        public String street;

        public static AddressDto of(Address a) {
            AddressDto d = new AddressDto();
            d.id = a.getId();
            d.version = a.getVersion();
            d.street = a.getStreet();
            return d;
        }

        public Address toEntity() {
            Address a = new Address(street);
            a.setId(id);
            a.setVersion(version);
            return a;
        }
    }

    public static class OrganizationDto {
        public long id;
        public long version;
        public String name;
        public String fullName;
        public long coordinatesId;
        public Long officialAddressId;
        public Long postalAddressId;
        public long annualTurnover;
        public Integer employeesCount;
        public int rating;
        public String type;

        public static OrganizationDto of(Organization o) {
            OrganizationDto d = new OrganizationDto();
            d.id = o.getId();
            d.version = o.getVersion();
            d.name = o.getName();
            d.fullName = o.getFullName();
            d.coordinatesId = o.getCoordinates() == null ? 0 : o.getCoordinates().getId();
            d.officialAddressId = o.getOfficialAddress() == null ? null : o.getOfficialAddress().getId();
            d.postalAddressId = o.getPostalAddress() == null ? null : o.getPostalAddress().getId();
            d.annualTurnover = o.getAnnualTurnover();
            d.employeesCount = o.getEmployeesCount();
            d.rating = o.getRating();
            d.type = o.getType() == null ? null : o.getType().name();
            return d;
        }

        /** Ссылки на координаты и адреса передаются идентификаторами; сервис сам подставляет сохранённые объекты. */
        public Organization toEntity() {
            Organization o = new Organization();
            o.setId(id);
            o.setVersion(version);
            o.setName(name);
            o.setFullName(fullName);
            if (coordinatesId != 0) {
                Coordinates c = new Coordinates();
                c.setId(coordinatesId);
                o.setCoordinates(c);
            }
            o.setOfficialAddress(reference(officialAddressId));
            o.setPostalAddress(reference(postalAddressId));
            o.setAnnualTurnover(annualTurnover);
            o.setEmployeesCount(employeesCount);
            o.setRating(rating);
            if (type != null && !type.isBlank()) {
                try {
                    o.setType(OrganizationType.valueOf(type.trim().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    throw new ru.astrosoup.islab.exceptions.BusinessException("Неизвестный тип организации: " + type);
                }
            }
            return o;
        }

        private static Address reference(Long id) {
            if (id == null) {
                return null;
            }
            Address a = new Address();
            a.setId(id);
            return a;
        }
    }

    public static class OrganizationPage {
        public long total;
        public List<OrganizationDto> items;
    }

    public static class MergeRequest {
        public long firstId;
        public long secondId;
        public String name;
        public String street;
    }

    public static class AbsorbRequest {
        public long acquirerId;
        public long targetId;
    }

    public static class ImportResultDto {
        public boolean success;
        public Long operationId;
        public Integer added;
        public List<String> errors;
    }

    public static class HistoryItem {
        public long id;
        public String status;
        public String user;
        public Integer added;
    }
}
