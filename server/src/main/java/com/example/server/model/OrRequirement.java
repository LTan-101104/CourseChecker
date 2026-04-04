package com.example.server.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;

/**
 * Composite node in the prerequisite tree.
 * ANY ONE of the children must be satisfied for this requirement to be met.
 *
 * Example: "MATH 131 OR MATH 132" is an OrRequirement
 * with two CourseRequirement children.
 */
@Entity
@DiscriminatorValue("OR")
public class OrRequirement extends Requirement {

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "parent_id")
    private List<Requirement> children = new ArrayList<>();

    public OrRequirement() {}

    public OrRequirement(List<Requirement> children) {
        this.children = children;
    }

    public List<Requirement> getChildren() { return children; }
    public void setChildren(List<Requirement> children) { this.children = children; }

    public void addChild(Requirement child) { this.children.add(child); }
}
