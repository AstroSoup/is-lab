package ru.astrosoup.islab.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import ru.astrosoup.islab.exceptions.EntityValidationException;
import ru.astrosoup.islab.repositories.OrganizationRepository;

import java.util.List;
import java.util.Locale;


@ApplicationScoped
public class UniquenessRules {

    @Inject
    OrganizationRepository organizations;

    public static String fullNameKey(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return null;
        }
        return fullName.trim().toLowerCase(Locale.ROOT);
    }

    public static String fullNameTaken(String fullName) {
        return "Организация с полным названием «" + fullName.trim() + "» уже существует";
    }

    public void requireUniqueFullName(String fullName, long excludeId) {
        if (fullNameKey(fullName) == null) {
            return;
        }
        if (organizations.existsByFullName(fullName.trim(), excludeId)) {
            throw new EntityValidationException(List.of(fullNameTaken(fullName)));
        }
    }

}
