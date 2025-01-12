package com.example.nagoyameshi.form;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ReviewForm {

    @NotNull(message = "評価を入力してください。")
    @Min(value = 1, message = "評価は1以上である必要があります。")
    @Max(value = 5, message = "評価は5以下である必要があります。")
    private Integer rating;

    @NotBlank(message = "コメントを入力してください。")
    private String comment;

    // ゲッター・セッター
    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
