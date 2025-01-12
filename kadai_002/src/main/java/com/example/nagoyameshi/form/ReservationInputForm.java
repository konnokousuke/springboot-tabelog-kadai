package com.example.nagoyameshi.form;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

public class ReservationInputForm {

    @NotNull(message = "予約日時を入力してください。")
    @Future(message = "予約日時は未来の日付を指定してください。")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime reservationDatetime;

    @NotNull(message = "予約人数を入力してください。")
    private Integer numberOfPeople;

    @NotNull(message = "店舗IDを入力してください。")  // 店舗IDのバリデーションメッセージ
    private Long storeId;  // storeIdフィールドを追加

    // ゲッターとセッター
    public LocalDateTime getReservationDatetime() {
        return reservationDatetime;
    }

    public void setReservationDatetime(LocalDateTime reservationDatetime) {
        this.reservationDatetime = reservationDatetime;
    }

    public Integer getNumberOfPeople() {
        return numberOfPeople;
    }

    public void setNumberOfPeople(Integer numberOfPeople) {
        this.numberOfPeople = numberOfPeople;
    }

    public Long getStoreId() {  // storeIdのゲッター
        return storeId;
    }

    public void setStoreId(Long storeId) {  // storeIdのセッター
        this.storeId = storeId;
    }
}