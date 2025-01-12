package com.example.nagoyameshi.form;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.example.nagoyameshi.validation.NotEmptyFile;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StoreRegisterForm {

    @NotBlank(message = "店舗名を入力してください。")
    private String name;

    @NotEmptyFile(message = "画像を選択してください。")
    private MultipartFile imageFile;

    @NotBlank(message = "説明を入力してください。")
    private String description;

    @NotNull(message = "価格を入力してください。")
    @Min(value = 1, message = "価格は1円以上にしてください。")
    private Long price;

    @NotBlank(message = "郵便番号を入力してください。")
    private String postalCode;

    @NotBlank(message = "住所を入力してください。")
    private String address;

    @NotBlank(message = "電話番号を入力してください。")
    private String phoneNumber;
    
    private List<Long> categories;

    @NotBlank(message = "営業時間（開始）を入力してください。")
    private String openingHours;

    @NotBlank(message = "営業時間（終了）を入力してください。")
    private String closingTime;

    private List<String> dayOff;
    
    // 画像URLを保持するフィールド
    private String imageUrl;
}
