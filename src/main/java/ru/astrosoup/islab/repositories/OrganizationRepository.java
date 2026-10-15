package ru.astrosoup.islab.repositories;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import ru.astrosoup.islab.entities.Address;
import ru.astrosoup.islab.entities.Coordinates;
import ru.astrosoup.islab.entities.Organization;
import ru.astrosoup.islab.entities.OrganizationType;

import java.util.*;

@ApplicationScoped
public class OrganizationRepository {

    @PersistenceContext(unitName = "main")
    private EntityManager em;

    public Organization find(long id) {
        return em.find(Organization.class, id);
    }

    public Organization findForUpdate(long id) {
        return em.find(Organization.class, id, LockModeType.PESSIMISTIC_WRITE);
    }

    public void persist(Organization organization) {
        em.persist(organization);
    }

    public void remove(Organization organization) {
        em.remove(organization);
    }

    public void flush() {
        em.flush();
    }

    public List<Organization> findAll() {
        return em.createQuery("SELECT o FROM Organization o ORDER BY o.id", Organization.class).getResultList();
    }

    public List<Integer> findAllRatings() {
        return em.createQuery("SELECT o.rating FROM Organization o", Integer.class).getResultList();
    }

    public List<OrganizationType> findAllTypes() {
        return em.createQuery("SELECT o.type FROM Organization o", OrganizationType.class).getResultList();
    }

    public long countByCoordinates(Coordinates coordinates) {
        return em.createQuery("SELECT COUNT(o) FROM Organization o WHERE o.coordinates = :c", Long.class)
                .setParameter("c", coordinates).getSingleResult();
    }

    public long countByAddress(Address address) {
        return em.createQuery(
                        "SELECT COUNT(o) FROM Organization o WHERE o.officialAddress = :a OR o.postalAddress = :a",
                        Long.class)
                .setParameter("a", address).getSingleResult();
    }

    public List<Organization> findPage(OrganizationFilter filter, List<SortSpec> sort, int offset, int limit) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Organization> cq = cb.createQuery(Organization.class);
        Root<Organization> root = cq.from(Organization.class);
        Paths p = new Paths(root);

        cq.select(root).where(predicates(cb, p, filter).toArray(new Predicate[0]));

        List<Order> orders = new ArrayList<>();
        if (sort != null) {
            for (SortSpec spec : sort) {
                Order o = order(cb, p, spec);
                if (o != null) orders.add(o);
            }
        }
        orders.add(cb.asc(root.get("id")));
        cq.orderBy(orders);

        TypedQuery<Organization> query = em.createQuery(cq);
        query.setFirstResult(Math.max(0, offset));
        query.setMaxResults(Math.max(1, limit));
        return query.getResultList();
    }

    public long count(OrganizationFilter filter) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Organization> root = cq.from(Organization.class);
        Paths p = new Paths(root);
        cq.select(cb.count(root)).where(predicates(cb, p, filter).toArray(new Predicate[0]));
        return em.createQuery(cq).getSingleResult();
    }

    private static final class Paths {
        final Root<Organization> root;
        final Join<Organization, Coordinates> coordinates;
        final Join<Organization, Address> official;
        final Join<Organization, Address> postal;

        Paths(Root<Organization> root) {
            this.root = root;
            this.coordinates = root.join("coordinates", JoinType.INNER);
            this.official = root.join("officialAddress", JoinType.LEFT);
            this.postal = root.join("postalAddress", JoinType.LEFT);
        }
    }

    private List<Predicate> predicates(CriteriaBuilder cb, Paths p, OrganizationFilter f) {
        List<Predicate> list = new ArrayList<>();
        if (f == null) return list;
        addLike(cb, list, p.root.<String>get("name"), f.name());
        addLike(cb, list, p.root.<String>get("fullName"), f.fullName());
        addLike(cb, list, p.official.<String>get("street"), f.officialAddress());
        addLike(cb, list, p.postal.<String>get("street"), f.postalAddress());

        if (hasText(f.type())) {
            String needle = f.type().trim().toLowerCase(Locale.ROOT);
            Set<OrganizationType> matching = EnumSet.noneOf(OrganizationType.class);
            for (OrganizationType t : OrganizationType.values()) {
                if (t.name().toLowerCase(Locale.ROOT).contains(needle)) matching.add(t);
            }
            list.add(matching.isEmpty() ? cb.disjunction() : p.root.<OrganizationType>get("type").in(matching));
        }
        return list;
    }

    private void addLike(CriteriaBuilder cb, List<Predicate> list, Expression<String> path, String text) {
        if (!hasText(text)) return;
        String pattern = "%" + escapeLike(text.trim().toLowerCase(Locale.ROOT)) + "%";
        list.add(cb.like(cb.lower(path), pattern, '\\'));
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private Order order(CriteriaBuilder cb, Paths p, SortSpec spec) {
        Expression<?> e = switch (spec.property()) {
            case "id", "name", "creationDate", "annualTurnover", "employeesCount", "rating", "fullName", "type" ->
                    p.root.get(spec.property());
            case "x" -> p.coordinates.get("x");
            case "y" -> p.coordinates.get("y");
            case "officialAddress" -> p.official.get("street");
            case "postalAddress" -> p.postal.get("street");
            default -> null;
        };
        if (e == null) return null;
        return spec.ascending() ? cb.asc(e) : cb.desc(e);
    }
}
