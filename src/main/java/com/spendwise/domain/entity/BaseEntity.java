package com.spendwise.domain.entity;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Base for all entities: UUID primary key and audit timestamps.
 * Subclasses are audited via {@link org.springframework.data.jpa.domain.support.AuditingEntityListener}.
 */
@MappedSuperclass // Tells JPA: “This class is not a table, but its fields should be mapped into child entity tables.”
@EntityListeners(AuditingEntityListener.class) // Telling Spring to automatically fill createdAt and updatedAt for me, so  dont need to do it manually
@Getter
@Setter
public abstract class   BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // Automatically generates a UUID for the primary key. Universally Unique Identifier. @GeneratedValue tells JPA/Hibernate to automatically generate a value for the field marked with @Id, so each entity gets a unique identifier that can be used as the primary key.
    private UUID id;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
