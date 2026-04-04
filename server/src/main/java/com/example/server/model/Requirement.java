package com.example.server.model;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

/**
 * Abstract base class for the Composite Pattern prerequisite tree.
 *
 * Uses SINGLE_TABLE inheritance: all requirement types (COURSE, AND, OR)
 * are stored in a single "requirement" table, distinguished by the
 * "requirement_type" discriminator column.
 *
 * Subclasses:
 * - {@link CourseRequirement} — Leaf node (a specific course)
 * - {@link AndRequirement}    — Composite: ALL children must be satisfied
 * - {@link OrRequirement}     — Composite: ANY child must be satisfied
 */
@Entity
@Table(name = "requirement")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "requirement_type", discriminatorType = DiscriminatorType.STRING)
public abstract class Requirement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
}
