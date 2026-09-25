package com.realestate.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "tasks")
public class Task {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String taskName;
    private String assignedTo;
    private String relatedCustomer;
    private String relatedProperty;
    private String priority = "Medium";
    private LocalDate dueDate;
    private String status = "Pending";
    @Column(length = 2000) private String notes;

    public Task() {}
    public Long getId() { return id; }
    public String getTaskName() { return taskName; }
    public void setTaskName(String value) { taskName = value; }
    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String value) { assignedTo = value; }
    public String getRelatedCustomer() { return relatedCustomer; }
    public void setRelatedCustomer(String value) { relatedCustomer = value; }
    public String getRelatedProperty() { return relatedProperty; }
    public void setRelatedProperty(String value) { relatedProperty = value; }
    public String getPriority() { return priority; }
    public void setPriority(String value) { priority = value; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate value) { dueDate = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public String getNotes() { return notes; }
    public void setNotes(String value) { notes = value; }
}
