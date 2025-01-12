package com.example.nagoyameshi.form;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class ReservationRegisterForm {

    @NotNull(message = "予約日時は必須です。")
    @Future(message = "予約日時は未来の日付を指定してください。")
    private LocalDateTime reservationDatetime;

    @NotNull(message = "予約人数は必須です。")
    @Min(value = 1, message = "予約人数は1人以上を指定してください。")
    private Integer numberOfPeople;

    @NotNull(message = "店舗IDは必須です。")
    private Long storeId;

    @NotNull(message = "会員IDは必須です。")
    private Integer memberId;

    // Getter and Setter methods
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

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public @NotNull(message = "会員IDは必須です。") Integer getMemberId() {
        return memberId;
    }

    public void setMemberId(Integer integer) {
        this.memberId = integer;
    }
}
