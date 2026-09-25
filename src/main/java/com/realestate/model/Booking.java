package com.realestate.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "bookings")
public class Booking {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true) private String bookingId;
    private String customer;
    private LocalDate bookingDate;
    private BigDecimal propertyPrice;
    private BigDecimal negotiatedPrice;
    private BigDecimal bookingAmount;
    private String agent;
    private String broker;
    private String status = "Booked";
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Property property;

    public Booking() {}
    public Long getId() { return id; }
    public String getBookingId() { return bookingId; }
    public void setBookingId(String value) { bookingId = value; }
    public String getCustomer() { return customer; }
    public void setCustomer(String value) { customer = value; }
    public LocalDate getBookingDate() { return bookingDate; }
    public void setBookingDate(LocalDate value) { bookingDate = value; }
    public BigDecimal getPropertyPrice() { return propertyPrice; }
    public void setPropertyPrice(BigDecimal value) { propertyPrice = value; }
    public BigDecimal getNegotiatedPrice() { return negotiatedPrice; }
    public void setNegotiatedPrice(BigDecimal value) { negotiatedPrice = value; }
    public BigDecimal getBookingAmount() { return bookingAmount; }
    public void setBookingAmount(BigDecimal value) { bookingAmount = value; }
    public String getAgent() { return agent; }
    public void setAgent(String value) { agent = value; }
    public String getBroker() { return broker; }
    public void setBroker(String value) { broker = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public Property getProperty() { return property; }
    public void setProperty(Property value) { property = value; }
}
