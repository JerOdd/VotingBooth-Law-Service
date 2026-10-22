package com.votingbooth.law.core.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class LawCategoryId implements Serializable {

    @Column(name = "law_id", nullable = false)
    private UUID lawId;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    protected LawCategoryId() {
    }

    public LawCategoryId(UUID lawId, UUID categoryId) {
        this.lawId = lawId;
        this.categoryId = categoryId;
    }

    public UUID getLawId() {
        return lawId;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LawCategoryId that)) {
            return false;
        }
        return Objects.equals(lawId, that.lawId) && Objects.equals(categoryId, that.categoryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lawId, categoryId);
    }

}
