package com.example.nagoyameshi.form;

import java.time.DayOfWeek;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StoreEditForm {

    @NotNull
    private Long storeId;

    @NotBlank(message = "店舗名を入力してください。")
    private String name;
    
    private MultipartFile imageFile;

    @NotBlank(message = "説明を入力してください。")
    private String description;

    @NotNull(message = "価格を入力してください。")
    @Min(value = 1, message = "価格を1円以上に設定してください。")
    private Long price;

    @NotBlank(message = "郵便番号を入力してください。")
    private String postalCode;

    @NotBlank(message = "住所を入力してください。")
    private String address;

    @NotBlank(message = "電話番号を入力してください。")
    private String phoneNumber;

    @NotBlank(message = "営業時間を入力してください。")
    private String openingHours;

    @NotBlank(message = "営業時間（終了）を入力してください。")
    private String closingTime; // 営業終了時間

    private Set<DayOfWeek> dayOffs; // 定休日（Enum型）
    private List<Long> categories; // カテゴリIDのリスト
    private List<String> dayOff;   // 定休日のリスト（日本語文字列）

    // 引数なしのデフォルトコンストラクター
    public StoreEditForm() {
    }

    // 全フィールドを初期化するカスタムコンストラクター
    public StoreEditForm(Long storeId, String name, MultipartFile imageFile, String description, Long price,
                         String postalCode, String address, String phoneNumber,
                         String openingHours, String closingTime, String closedDays, List<Long> categories) {
        this.storeId = storeId;
        this.name = name;
        this.imageFile = imageFile;
        this.description = description;
        this.price = price;
        this.postalCode = postalCode;
        this.address = address;
        this.phoneNumber = phoneNumber;
        
        // 営業開始時間を抽出（例: "09:00 - 18:00"から"09:00"を抽出）
        this.openingHours = openingHours != null && openingHours.contains(" - ")
                ? openingHours.split(" - ")[0]
                : openingHours;
        
        this.closingTime = closingTime;
        this.dayOff = closedDays != null ? List.of(closedDays.split(",")) : null; // カンマ区切りからリストへ変換
        this.categories = categories;
        this.dayOffs = convertClosedDaysToSet(closedDays); // 定休日（Enum型）へ変換
    }

    private Set<DayOfWeek> convertClosedDaysToSet(String closedDays) {
        if (closedDays == null || closedDays.isBlank()) {
            return null;
        }

        Map<String, DayOfWeek> dayOfWeekMap = getJapaneseDayOfWeekMap();

        return Stream.of(closedDays.split(","))
                .map(String::trim)
                .map(dayOfWeekMap::get)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Map<String, DayOfWeek> getJapaneseDayOfWeekMap() {
        Map<String, DayOfWeek> map = new HashMap<>();
        map.put("日", DayOfWeek.SUNDAY);
        map.put("月", DayOfWeek.MONDAY);
        map.put("火", DayOfWeek.TUESDAY);
        map.put("水", DayOfWeek.WEDNESDAY);
        map.put("木", DayOfWeek.THURSDAY);
        map.put("金", DayOfWeek.FRIDAY);
        map.put("土", DayOfWeek.SATURDAY);
        return map;
    }
}
