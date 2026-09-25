package com.realestate.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "customers")
public class Customer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id", nullable = false, unique = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Lead lead;
    private String name;
    private String email;
    private String mobileNumber;
    private String interestedProperty;
    private LocalDate createdDate = LocalDate.now();

    public Customer() {}
    public Customer(Lead lead) {
        this.lead = lead;
        name = lead.getCustomerName();
        email = lead.getEmail();
        mobileNumber = lead.getMobileNumber();
        interestedProperty = lead.getInterestedProperty();
    }
    public Long getId() { return id; }
    public Long getLeadId() { return lead.getId(); }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getMobileNumber() { return mobileNumber; }
    public String getInterestedProperty() { return interestedProperty; }
    public LocalDate getCreatedDate() { return createdDate; }
}
