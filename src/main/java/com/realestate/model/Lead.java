package com.realestate.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "leads")
public class Lead {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String customerName;
    private String mobileNumber;
    private String whatsappNumber;
    private String email;
    private String interestedProperty;
    private String preferredLocation;
    private String propertyType;
    private BigDecimal minimumBudget;
    private BigDecimal maximumBudget;
    private Boolean advancePaidEnabled = false;
    @Column(precision = 15, scale = 2)
    private BigDecimal totalAssetValue;
    private Long paymentRevision = 0L;
    public BigDecimal getTotalAssetValue() { return totalAssetValue; }
    public void setTotalAssetValue(BigDecimal value) { totalAssetValue = value; }
    public Long getPaymentRevision() { return paymentRevision == null ? 0L : paymentRevision; }
    public void setPaymentRevision(Long value) { paymentRevision = value; }
    public Boolean getAdvancePaidEnabled() { return Boolean.TRUE.equals(advancePaidEnabled); }
    public void setAdvancePaidEnabled(Boolean value) { advancePaidEnabled = Boolean.TRUE.equals(value); }
    private String leadSource;
    private String assignedAgent;
    private String leadStatus = "New";
    private String category = "Warm";
    private LocalDate nextFollowUpDate;
    private LocalDate createdDate = LocalDate.now();
    @Column(length = 2000) private String notes;

    public Lead() {}
    public Long getId() { return id; }
    public void setId(Long value) { id = value; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String value) { customerName = value; }
    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String value) { mobileNumber = value; }
    public String getWhatsappNumber() { return whatsappNumber; }
    public void setWhatsappNumber(String value) { whatsappNumber = value; }
    public String getEmail() { return email; }
    public void setEmail(String value) { email = value; }
    public String getInterestedProperty() { return interestedProperty; }
    public void setInterestedProperty(String value) { interestedProperty = value; }
    public String getPreferredLocation() { return preferredLocation; }
    public void setPreferredLocation(String value) { preferredLocation = value; }
    public String getPropertyType() { return propertyType; }
    public void setPropertyType(String value) { propertyType = value; }
    public BigDecimal getMinimumBudget() { return minimumBudget; }
    public void setMinimumBudget(BigDecimal value) { minimumBudget = value; }
    public BigDecimal getMaximumBudget() { return maximumBudget; }
    public void setMaximumBudget(BigDecimal value) { maximumBudget = value; }
    public String getLeadSource() { return leadSource; }
    public void setLeadSource(String value) { leadSource = value; }
    public String getAssignedAgent() { return assignedAgent; }
    public void setAssignedAgent(String value) { assignedAgent = value; }
    public String getLeadStatus() { return leadStatus; }
    public void setLeadStatus(String value) { leadStatus = value; }
    public String getCategory() { return category; }
    public void setCategory(String value) { category = value; }
    public LocalDate getNextFollowUpDate() { return nextFollowUpDate; }
    public void setNextFollowUpDate(LocalDate value) { nextFollowUpDate = value; }
    public LocalDate getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDate value) { createdDate = value; }
    public String getNotes() { return notes; }
    public void setNotes(String value) { notes = value; }
}
