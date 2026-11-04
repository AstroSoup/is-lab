package ru.astrosoup.islab.repositories;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import ru.astrosoup.islab.entities.ImportOperation;

import java.util.List;

@ApplicationScoped
public class ImportOperationRepository {

    @PersistenceContext(unitName = "main")
    private EntityManager em;

    public void persist(ImportOperation operation) {
        em.persist(operation);
    }

    public void flush() {
        em.flush();
    }

    public List<ImportOperation> findPage(int offset, int limit) {
        return em.createQuery("SELECT o FROM ImportOperation o ORDER BY o.id DESC", ImportOperation.class)
                .setFirstResult(Math.max(0, offset)).setMaxResults(Math.max(1, limit)).getResultList();
    }

    public List<ImportOperation> findPageByUser(String userName, int offset, int limit) {
        return em.createQuery("SELECT o FROM ImportOperation o WHERE o.userName = :u ORDER BY o.id DESC",
                        ImportOperation.class)
                .setParameter("u", userName)
                .setFirstResult(Math.max(0, offset)).setMaxResults(Math.max(1, limit)).getResultList();
    }

    public long count() {
        return em.createQuery("SELECT COUNT(o) FROM ImportOperation o", Long.class).getSingleResult();
    }

    public long countByUser(String userName) {
        return em.createQuery("SELECT COUNT(o) FROM ImportOperation o WHERE o.userName = :u", Long.class)
                .setParameter("u", userName).getSingleResult();
    }
}
