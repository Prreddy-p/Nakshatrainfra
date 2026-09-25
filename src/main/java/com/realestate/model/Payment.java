package com.realestate.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "payments")
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String customer;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Lead lead;
    public Lead getLead() { return lead; }
    public void setLead(Lead value) { lead = value; }
    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
    public Long getLeadId() { return lead == null ? null : lead.getId(); }
    private String property;
    private String booking;
    private String paymentType;
    private BigDecimal amount;
    private LocalDate dueDate;
    private LocalDate paidDate;
    private String paymentMode;
    private String transactionNumber;
    private String receiptNumber;
    private String status = "Pending";
    @Column(length = 1000) private String notes;

    public Payment() {}
    public Long getId() { return id; }
    public String getCustomer() { return customer; }
    public void setCustomer(String value) { customer = value; }
    public String getProperty() { return property; }
    public void setProperty(String value) { property = value; }
    public String getBooking() { return booking; }
    public void setBooking(String value) { booking = value; }
    public String getPaymentType() { return paymentType; }
    public void setPaymentType(String value) { paymentType = value; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal value) { amount = value; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate value) { dueDate = value; }
    public LocalDate getPaidDate() { return paidDate; }
    public void setPaidDate(LocalDate value) { paidDate = value; }
    public String getPaymentMode() { return paymentMode; }
    public void setPaymentMode(String value) { paymentMode = value; }
    public String getTransactionNumber() { return transactionNumber; }
    public void setTransactionNumber(String value) { transactionNumber = value; }
    public String getReceiptNumber() { return receiptNumber; }
    public void setReceiptNumber(String value) { receiptNumber = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public String getNotes() { return notes; }
    public void setNotes(String value) { notes = value; }
}
