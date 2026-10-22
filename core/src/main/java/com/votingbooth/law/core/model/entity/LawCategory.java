package com.votingbooth.law.core.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "law_category")
public class LawCategory {

    @EmbeddedId
    private LawCategoryId id;

    @Enumerated(EnumType.STRING)
    @Column(name = "origin", nullable = false)
    private CategoryOrigin origin;

    protected LawCategory() {
    }

    public LawCategory(LawCategoryId id, CategoryOrigin origin) {
        this.id = id;
        this.origin = origin;
    }

    public LawCategoryId getId() {
        return id;
    }

    public CategoryOrigin getOrigin() {
        return origin;
    }

}
