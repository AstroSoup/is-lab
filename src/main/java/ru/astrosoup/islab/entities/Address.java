package ru.astrosoup.islab.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;


@Entity
@Table(name = "addresses")
@Cacheable(true)
@Getter
@Setter
@NoArgsConstructor
public class Address implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private long id;

    @Pattern(regexp = ".*\\S.*", message = "Улица не может быть пустой строкой")
    @Column(name = "street")
    private String street;

    public Address(String street) {
        this.street = street;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof Address other && id != 0 && id == other.id;
    }

    @Override
    public int hashCode() {
        return Address.class.hashCode();
    }

    @Override
    public String toString() {
        return "#" + id + (street == null ? " (улица не указана)" : " " + street);
    }
}
