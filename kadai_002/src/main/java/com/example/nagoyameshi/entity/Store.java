package com.example.nagoyameshi.entity;

import java.sql.Timestamp;
import java.time.LocalTime;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "stores")
@Data
public class Store {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_id")
    private Long storeId;
    
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "store_category",
        joinColumns = @JoinColumn(name = "store_id"),
        inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> categories;
    
    @Column(name = "store_name")
    private String storeName;
    
    @Column(name = "image_filename")
    private String imageFilename;
    
    @Column(name = "rating", columnDefinition = "INT CHECK (rating BETWEEN 1 AND 5) DEFAULT 1")
    private Integer rating;
    
    @Column(name = "description", nullable = false)
    private String description;
    
    @Column(name = "price", nullable = false)
    private int price;
    
    @Column(name = "postal_code", nullable = false)
    private String postalCode;
    
    @Column(name = "address", nullable = false)
    private String address;
    
    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;
    
    @Column(name = "opening_hours", nullable = false)
    private String openingHours;
    
    @Column(name = "closed_days", nullable = false)    
    private String closedDays;
    
    @Column(name = "closing_time", nullable = false)
    private LocalTime closingTime;
    
    @Column(name = "created_at", insertable = false, updatable = false)
    private Timestamp createdAt;
    
    @Column(name = "updated_at", insertable = false, updatable = false)
    private Timestamp updatedAt;
}