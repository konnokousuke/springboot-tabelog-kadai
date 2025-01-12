package com.example.nagoyameshi.service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.nagoyameshi.entity.Member;
import com.example.nagoyameshi.entity.Reservation;
import com.example.nagoyameshi.entity.Store;
import com.example.nagoyameshi.form.ReservationInputForm;
import com.example.nagoyameshi.repository.MemberRepository;
import com.example.nagoyameshi.repository.ReservationRepository;
import com.example.nagoyameshi.repository.StoreRepository;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final MemberRepository memberRepository;
    private final StoreRepository storeRepository;

    // 英語から日本語の曜日マッピング
    private static final Map<DayOfWeek, String> JAPANESE_DAY_MAP = new HashMap<>() {{
        put(DayOfWeek.MONDAY, "月");
        put(DayOfWeek.TUESDAY, "火");
        put(DayOfWeek.WEDNESDAY, "水");
        put(DayOfWeek.THURSDAY, "木");
        put(DayOfWeek.FRIDAY, "金");
        put(DayOfWeek.SATURDAY, "土");
        put(DayOfWeek.SUNDAY, "日");
    }};

    @Autowired
    public ReservationService(ReservationRepository reservationRepository, MemberRepository memberRepository, StoreRepository storeRepository) {
        this.reservationRepository = reservationRepository;
        this.memberRepository = memberRepository;
        this.storeRepository = storeRepository;
    }

    public String isReservationDateTimeValid(Store store, LocalDateTime reservationDatetime) {
        // 営業開始時間と終了時間をパース
        LocalTime openingTime = LocalTime.parse(store.getOpeningHours().split(" - ")[0]);
        LocalTime closingTime = LocalTime.parse(store.getOpeningHours().split(" - ")[1]);

        // 営業終了1時間前を計算
        LocalTime closingTimeWithMargin = closingTime.minusHours(1);

        // 定休日をチェック
        List<String> closedDays = Arrays.asList(store.getClosedDays().split(","));
        String reservationDayInJapanese = JAPANESE_DAY_MAP.get(reservationDatetime.getDayOfWeek());
        if (closedDays.contains(reservationDayInJapanese)) {
            return "定休日は指定できません。";
        }

        // 営業時間外のチェック
        if (reservationDatetime.toLocalTime().isBefore(openingTime) || reservationDatetime.toLocalTime().isAfter(closingTime)) {
            return "営業時間外です。";
        }

        // 営業終了1時間前以降、終了時間の1分前までのチェック
        if (!reservationDatetime.toLocalTime().isBefore(closingTimeWithMargin))  {
            return "営業終了1時間以上前を指定してください。";
        }

        return null; // エラーなし
    }

    public String validateReservation(ReservationInputForm form) {
        Store store = storeRepository.findById(form.getStoreId())
                        .orElseThrow(() -> new IllegalArgumentException("Invalid store ID: " + form.getStoreId()));

        if (form.getReservationDatetime().isBefore(LocalDateTime.now())) {
            return "指定した日時は無効です。";
        }

        String errorMessage = isReservationDateTimeValid(store, form.getReservationDatetime());
        if (errorMessage != null) {
            return errorMessage;
        }

        return null;
    }

    public void create(Reservation reservation, Integer memberId, Long storeId) {
        Member member = memberRepository.findById(memberId)
                            .orElseThrow(() -> new IllegalArgumentException("Invalid member ID: " + memberId));

        Store store = storeRepository.findById(storeId)
                            .orElseThrow(() -> new IllegalArgumentException("Invalid store ID: " + storeId));

        reservation.setMember(member);
        reservation.setStore(store);

        reservationRepository.save(reservation);
    }
    
    // 予約削除
    public boolean cancelReservation(Long reservationId) {
        if (reservationRepository.existsById(reservationId)) {
            reservationRepository.deleteById(reservationId);
            return true; // 削除成功
        }
        return false; // 予約が存在しない場合
    }
}