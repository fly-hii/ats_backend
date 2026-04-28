package com.example.ATSCIRCLE.Models.Sales;

import org.springframework.data.mongodb.core.mapping.Field;

/**
 * Shared Address model for Sales module
 * Used by Company, Client, and other entities as an embedded document
 */
public class Address {
    
    @Field("streetLine1")
    private String streetLine1;
    
    @Field("streetLine2")
    private String streetLine2;
    
    @Field("city")
    private String city;
    
    @Field("state")
    private String state;
    
    @Field("country")
    private String country;
    
    @Field("zipcode")
    private String zipcode;

    // Constructors
    public Address() {}

    public Address(String streetLine1, String streetLine2, 
                   String city, String state, String country, String zipcode) {
        this.streetLine1 = streetLine1;
        this.streetLine2 = streetLine2;
        this.city = city;
        this.state = state;
        this.country = country;
        this.zipcode = zipcode;
    }

    // Getters and Setters
    public String getStreetLine1() {
        return streetLine1;
    }

    public void setStreetLine1(String streetLine1) {
        this.streetLine1 = streetLine1;
    }

    public String getStreetLine2() {
        return streetLine2;
    }

    public void setStreetLine2(String streetLine2) {
        this.streetLine2 = streetLine2;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getZipcode() {
        return zipcode;
    }

    public void setZipcode(String zipcode) {
        this.zipcode = zipcode;
    }

    /**
     * Deep copy constructor for migration purposes
     * Returns null if source is null
     */
    public static Address copy(Address source) {
        if (source == null) {
            return null;
        }
        Address dest = new Address();
        dest.setStreetLine1(source.getStreetLine1());
        dest.setStreetLine2(source.getStreetLine2());
        dest.setCity(source.getCity());
        dest.setState(source.getState());
        dest.setCountry(source.getCountry());
        dest.setZipcode(source.getZipcode());
        return dest;
    }

    @Override
    public String toString() {
        return "Address{" +
                "streetLine1='" + streetLine1 + '\'' +
                ", streetLine2='" + streetLine2 + '\'' +
                ", city='" + city + '\'' +
                ", state='" + state + '\'' +
                ", country='" + country + '\'' +
                ", zipcode='" + zipcode + '\'' +
                '}';
    }
}