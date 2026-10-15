package ru.astrosoup.islab.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "organizations")
@Cacheable(false)
@Getter
@Setter
@NoArgsConstructor
public class Organization implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private long id;

    @NotBlank(message = "Название организации не может быть пустым")
    @Column(name = "name", nullable = false)
    private String name;

    @NotNull(message = "Координаты обязательны")
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "coordinates_id", nullable = false)
    private Coordinates coordinates;

    @NotNull(message = "Дата создания обязательна")
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "creation_date", nullable = false, updatable = false)
    private Date creationDate;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "official_address_id")
    private Address officialAddress;

    @Positive(message = "Годовой оборот должен быть больше 0")
    @Column(name = "annual_turnover", nullable = false)
    private long annualTurnover;

    @NotNull(message = "Количество сотрудников обязательно")
    @Positive(message = "Количество сотрудников должно быть больше 0")
    @Column(name = "employees_count", nullable = false)
    private Integer employeesCount;

    @Positive(message = "Рейтинг должен быть больше 0")
    @Column(name = "rating", nullable = false)
    private int rating;

    @Pattern(regexp = ".*\\S.*", message = "Полное название не может быть пустой строкой")
    @Column(name = "full_name")
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "organization_type")
    private OrganizationType type;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "postal_address_id")
    private Address postalAddress;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @PrePersist
    void onCreate() {
        if (creationDate == null) {
            creationDate = new Date();
        }
    }

    public void copyEditableFieldsFrom(Organization other) {
        this.name = other.name;
        this.coordinates = other.coordinates;
        this.officialAddress = other.officialAddress;
        this.annualTurnover = other.annualTurnover;
        this.employeesCount = other.employeesCount;
        this.rating = other.rating;
        this.fullName = other.fullName;
        this.type = other.type;
        this.postalAddress = other.postalAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof Organization other && id != 0 && id == other.id;
    }

    @Override
    public int hashCode() {
        return Organization.class.hashCode();
    }

    @Override
    public String toString() {
        return "#" + id + " " + name;
    }
}
