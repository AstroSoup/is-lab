package ru.astrosoup.islab.repositories;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import ru.astrosoup.islab.entities.Coordinates;

import java.util.List;

@ApplicationScoped
public class CoordinatesRepository {

    @PersistenceContext(unitName = "main")
    private EntityManager em;

    public Coordinates find(long id) {
        return em.find(Coordinates.class, id);
    }

    public List<Coordinates> findAll() {
        return em.createQuery("SELECT c FROM Coordinates c ORDER BY c.id", Coordinates.class).getResultList();
    }

    public void persist(Coordinates c) {
        em.persist(c);
    }

    public void remove(Coordinates c) {
        em.remove(c);
    }

    public void flush() {
        em.flush();
    }
}
