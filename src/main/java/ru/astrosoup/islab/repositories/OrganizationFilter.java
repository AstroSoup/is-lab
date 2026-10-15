package ru.astrosoup.islab.repositories;

public record OrganizationFilter(String name, String fullName, String officialAddress,
                                 String postalAddress, String type) {

    public static OrganizationFilter empty() {
        return new OrganizationFilter(null, null, null, null, null);
    }

    public static OrganizationFilter byName(String name) {
        return new OrganizationFilter(name, null, null, null, null);
    }
}
