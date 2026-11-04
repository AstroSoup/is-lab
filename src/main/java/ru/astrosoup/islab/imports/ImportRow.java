package ru.astrosoup.islab.imports;

import ru.astrosoup.islab.entities.OrganizationType;

public record ImportRow(int line,
                        String name,
                        String fullName,
                        double x,
                        int y,
                        long annualTurnover,
                        int employeesCount,
                        int rating,
                        OrganizationType type,
                        String officialStreet,
                        String postalStreet) {
}
