package ru.astrosoup.islab.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Entity
@Table(name = "coordinates")
@Cacheable(true)
@Getter
@Setter
@NoArgsConstructor
public class Coordinates implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private long id;

    @NotNull(message = "Координата X обязательна")
    @DecimalMax(value = "580", message = "Координата X не может быть больше 580")
    @Column(name = "x", nullable = false)
    private Double x;

    @Max(value = 885, message = "Координата Y не может быть больше 885")
    @Column(name = "y", nullable = false)
    private int y;

    @AssertTrue(message = "Координата X должна быть конечным числом")
    public boolean isXFinite() {
        return x == null || Double.isFinite(x);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof Coordinates other && id != 0 && id == other.id;
    }

    @Override
    public int hashCode() {
        return Coordinates.class.hashCode();
    }

    @Override
    public String toString() {
        return "#" + id + " (x=" + x + ", y=" + y + ")";
    }
}
