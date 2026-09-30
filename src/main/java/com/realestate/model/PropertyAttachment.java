package com.realestate.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "property_attachments")
public class PropertyAttachment {
    @Id private String id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private Property property;
    private String fileName;
    private long size;
    private Instant uploadedAt;
    @Lob @Column(nullable = false, length = 10485760)
    private byte[] content;
    protected PropertyAttachment() {}
    public PropertyAttachment(Property property, String fileName, byte[] content) {
        this.id = java.util.UUID.randomUUID().toString(); this.property = property;
        this.fileName = fileName; this.content = content; this.size = content.length;
        this.uploadedAt = Instant.now();
    }
    public String getId() { return id; }
    public String getFileName() { return fileName; }
    public long getSize() { return size; }
    public Instant getUploadedAt() { return uploadedAt; }
    public byte[] getContent() { return content; }
}
