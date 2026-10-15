package ru.astrosoup.islab.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ru.astrosoup.islab.entities.User;
import ru.astrosoup.islab.exceptions.BusinessException;
import ru.astrosoup.islab.repositories.UserRepository;

@ApplicationScoped
@Transactional
public class AuthService {
    public static final int MIN_NAME = 3;
    public static final int MAX_NAME = 50;
    public static final int MIN_PASSWORD = 6;

    @Inject
    UserRepository users;

    public void register(String name, String password) {
        String login = name == null ? "" : name.trim();
        if (login.length() < MIN_NAME || login.length() > MAX_NAME) {
            throw new BusinessException("Имя пользователя должно содержать от " + MIN_NAME + " до " + MAX_NAME + " символов");
        }
        if (password == null || password.length() < MIN_PASSWORD) {
            throw new BusinessException("Пароль должен содержать не менее " + MIN_PASSWORD + " символов");
        }
        if (users.find(login) != null) {
            throw new BusinessException("Пользователь «" + login + "» уже существует");
        }
        users.persist(new User(login, PasswordHasher.hash(password)));
        PersistenceFailures.flush(users::flush, "Пользователь «" + login + "» уже существует");
    }

    public boolean authenticate(String name, String password) {
        if (name == null || password == null) return false;
        User user = users.find(name.trim());
        return user != null && PasswordHasher.matches(password, user.getPasswordHash());
    }
}
