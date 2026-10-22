package com.votingbooth.law.core.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "laws")
public class Law {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    protected Law() {
    }

    public Law(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

}
