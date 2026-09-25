package com.realestate.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "properties")
public class Property {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true) private String propertyId;
    @Column(nullable = false) private String propertyName;
    private String propertyType;
    private String projectName;
    private String ownerName;
    private String mobileNumber;
    private String address;
    private String location;
    private String city;
    private String state;
    private String plotNumber;
    private String surveyNumber;
    private String facing;
    private Integer bedrooms;
    private Integer bathrooms;
    private Double propertySize;
    private BigDecimal price;
    private BigDecimal pricePerSqFt;
    @Column(length = 2000) private String description;
    private String imageUrl;
    @Column(nullable = false) private String status = "Available";

    public Property() {}
    public Long getId() { return id; }
    public String getPropertyId() { return propertyId; }
    public void setPropertyId(String value) { propertyId = value; }
    public String getPropertyName() { return propertyName; }
    public void setPropertyName(String value) { propertyName = value; }
    public String getPropertyType() { return propertyType; }
    public void setPropertyType(String value) { propertyType = value; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String value) { projectName = value; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String value) { ownerName = value; }
    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String value) { mobileNumber = value; }
    public String getAddress() { return address; }
    public void setAddress(String value) { address = value; }
    public String getLocation() { return location; }
    public void setLocation(String value) { location = value; }
    public String getCity() { return city; }
    public void setCity(String value) { city = value; }
    public String getState() { return state; }
    public void setState(String value) { state = value; }
    public String getPlotNumber() { return plotNumber; }
    public void setPlotNumber(String value) { plotNumber = value; }
    public String getSurveyNumber() { return surveyNumber; }
    public void setSurveyNumber(String value) { surveyNumber = value; }
    public String getFacing() { return facing; }
    public void setFacing(String value) { facing = value; }
    public Integer getBedrooms() { return bedrooms; }
    public void setBedrooms(Integer value) { bedrooms = value; }
    public Integer getBathrooms() { return bathrooms; }
    public void setBathrooms(Integer value) { bathrooms = value; }
    public Double getPropertySize() { return propertySize; }
    public void setPropertySize(Double value) { propertySize = value; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal value) { price = value; }
    public BigDecimal getPricePerSqFt() { return pricePerSqFt; }
    public void setPricePerSqFt(BigDecimal value) { pricePerSqFt = value; }
    public String getDescription() { return description; }
    public void setDescription(String value) { description = value; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String value) { imageUrl = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
}
