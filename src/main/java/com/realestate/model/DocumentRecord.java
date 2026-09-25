package com.realestate.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
public class DocumentRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String documentName;
    private String relatedType;
    private String relatedId;
    private String fileUrl;
    private String status = "Uploaded";
    private LocalDateTime uploadedAt = LocalDateTime.now();

    public DocumentRecord() {}
    public Long getId() { return id; }
    public String getDocumentName() { return documentName; }
    public void setDocumentName(String value) { documentName = value; }
    public String getRelatedType() { return relatedType; }
    public void setRelatedType(String value) { relatedType = value; }
    public String getRelatedId() { return relatedId; }
    public void setRelatedId(String value) { relatedId = value; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String value) { fileUrl = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime value) { uploadedAt = value; }
}
