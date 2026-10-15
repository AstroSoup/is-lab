package ru.astrosoup.islab.repositories;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import ru.astrosoup.islab.entities.Address;

import java.util.List;

@ApplicationScoped
public class AddressRepository {

    @PersistenceContext(unitName = "main")
    private EntityManager em;

    public Address find(long id) {
        return em.find(Address.class, id);
    }

    public List<Address> findAll() {
        return em.createQuery("SELECT a FROM Address a ORDER BY a.id", Address.class).getResultList();
    }

    public Address findByStreet(String street) {
        List<Address> list = em.createQuery("SELECT a FROM Address a WHERE a.street = :s ORDER BY a.id", Address.class)
                .setParameter("s", street).setMaxResults(1).getResultList();
        return list.isEmpty() ? null : list.get(0);
    }

    public void persist(Address a) {
        em.persist(a);
    }

    public void remove(Address a) {
        em.remove(a);
    }

    public void flush() {
        em.flush();
    }
}
