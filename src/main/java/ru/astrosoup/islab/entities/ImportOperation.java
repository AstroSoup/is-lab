package ru.astrosoup.islab.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "import_operations")
@Cacheable(false)
@Getter
@Setter
@NoArgsConstructor
public class ImportOperation implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private long id;

    @Column(name = "user_name", nullable = false, updatable = false)
    private String userName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ImportStatus status;

    @Column(name = "added_count")
    private Integer addedCount;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "error_message")
    private String errorMessage;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Date createdAt;

    public static ImportOperation success(String userName, String fileName, int addedCount) {
        ImportOperation op = new ImportOperation();
        op.userName = userName;
        op.fileName = fileName;
        op.status = ImportStatus.SUCCESS;
        op.addedCount = addedCount;
        return op;
    }

    public static ImportOperation failure(String userName, String fileName, String errorMessage) {
        ImportOperation op = new ImportOperation();
        op.userName = userName;
        op.fileName = fileName;
        op.status = ImportStatus.FAILED;
        op.errorMessage = errorMessage;
        return op;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = new Date();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof ImportOperation other && id != 0 && id == other.id;
    }

    @Override
    public int hashCode() {
        return ImportOperation.class.hashCode();
    }
}
