package com.realestate.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonProperty;

@MappedSuperclass
public abstract class OwnedRecord {
    @Column(updatable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createdByUserId;
    public Long getCreatedByUserId() { return createdByUserId; }
    public void preserveOwner(OwnedRecord original) { createdByUserId = original.getCreatedByUserId(); }
    @PrePersist
    protected void captureOwner() {
        if (createdByUserId == null) createdByUserId = com.realestate.security.RecordAccess.userId();
    }
}
