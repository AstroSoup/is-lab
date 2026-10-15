package ru.astrosoup.islab.repositories;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import ru.astrosoup.islab.entities.User;

@ApplicationScoped
public class UserRepository {

    @PersistenceContext(unitName = "main")
    private EntityManager em;

    public User find(String name) {
        return em.find(User.class, name);
    }

    public void persist(User user) {
        em.persist(user);
    }

    public void flush() {
        em.flush();
    }
}
