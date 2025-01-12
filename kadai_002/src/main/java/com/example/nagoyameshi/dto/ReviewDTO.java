package com.example.nagoyameshi.dto;

import java.sql.Timestamp;

public class ReviewDTO {
    private Long id;
    private Long storeId;
    private String username;
    private int rating;
    private String comment;
    private Timestamp createdAt;

    // コンストラクタ
    public ReviewDTO(Long id, Long storeId, String username, int rating, String comment, Timestamp createdAt) {
        this.id = id;
        this.storeId = storeId;
        this.username = username;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getStoreId() {
        return storeId;
    }

    public String getUsername() {
        return username;
    }

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }
}

