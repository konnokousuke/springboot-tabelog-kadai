package com.example.nagoyameshi.controller;

import java.sql.Timestamp;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.nagoyameshi.entity.Member;
import com.example.nagoyameshi.entity.Reservation;
import com.example.nagoyameshi.entity.Store;
import com.example.nagoyameshi.form.ReservationInputForm;
import com.example.nagoyameshi.form.ReservationRegisterForm;
import com.example.nagoyameshi.repository.MemberRepository;
import com.example.nagoyameshi.repository.ReservationRepository;
import com.example.nagoyameshi.repository.ReviewRepository;
import com.example.nagoyameshi.repository.StoreRepository;
import com.example.nagoyameshi.security.MemberDetailsImpl;

import jakarta.validation.Valid;

@Controller
public class ReservationController {

    private final ReservationRepository reservationRepository;
    private final StoreRepository storeRepository;
    private final MemberRepository memberRepository;
    private final ReviewRepository reviewRepository;

    public ReservationController(ReservationRepository reservationRepository, StoreRepository storeRepository, MemberRepository memberRepository, ReviewRepository reviewRepository) {
        this.reservationRepository = reservationRepository;
        this.storeRepository = storeRepository;
        this.memberRepository = memberRepository;
        this.reviewRepository = reviewRepository;
    }

    @GetMapping("/reservations")
    public String index(@AuthenticationPrincipal MemberDetailsImpl memberDetailsImpl,
                        @PageableDefault(page = 0, size = 10, sort = "reservationId", direction = Direction.ASC) Pageable pageable,
                        Model model) {
        Member member = memberDetailsImpl.getMember();
        Page<Reservation> reservationPage = reservationRepository.findByMemberOrderByReservationIdDesc(member, pageable);

        model.addAttribute("reservationPage", reservationPage);

        return "reservations/index";
    }

    @GetMapping("/stores/{id}/reservations")
    public String storeReservations(@PathVariable("id") Long storeId,
                                    @PageableDefault(page = 0, size = 10, sort = "reservationId", direction = Direction.ASC) Pageable pageable,
                                    Model model) {
        Store store = storeRepository.findById(storeId)
                        .orElseThrow(() -> new IllegalArgumentException("Invalid store ID: " + storeId));

        Page<Reservation> reservationPage = reservationRepository.findByStore(store, pageable);

        model.addAttribute("reservationPage", reservationPage);
        model.addAttribute("store", store);

        return "reservations/store_reservations"; // 
    }

    @PostMapping("/stores/{id}/reservations/input")
    public String input(@PathVariable("id") Long storeId,
                        @Valid ReservationInputForm form,
                        BindingResult bindingResult,
                        @AuthenticationPrincipal MemberDetailsImpl memberDetailsImpl,
                        Model model) {
        Store store = storeRepository.findById(storeId)
                        .orElseThrow(() -> new IllegalArgumentException("Invalid store ID: " + storeId));

        if (bindingResult.hasErrors()) {
            model.addAttribute("store", store);
            model.addAttribute("errorMessage", "入力にエラーがあります。");
            return "stores/show";
        }

        Member member = memberDetailsImpl.getMember();
        ReservationRegisterForm registerForm = new ReservationRegisterForm();
        registerForm.setReservationDatetime(form.getReservationDatetime());
        registerForm.setNumberOfPeople(form.getNumberOfPeople());
        registerForm.setStoreId(storeId);
        registerForm.setMemberId(member.getId());

        model.addAttribute("registerForm", registerForm);
        model.addAttribute("store", store);

        return "reservations/confirm";
    }

    @PostMapping("/stores/{id}/reservations/create")
    public String create(@PathVariable("id") Long storeId,
                         @Valid ReservationRegisterForm registerForm,
                         BindingResult bindingResult,
                         Model model) {
        Store store = storeRepository.findById(storeId)
                        .orElseThrow(() -> new IllegalArgumentException("Invalid store ID: " + storeId));

        if (bindingResult.hasErrors()) {
            model.addAttribute("store", store);
            model.addAttribute("errorMessage", "入力にエラーがあります。");
            return "reservations/confirm";
        }

        // Reservationエンティティのインスタンスを作成し、フォームのデータを設定
        Reservation reservation = new Reservation();
        Timestamp reservationTimestamp = Timestamp.valueOf(registerForm.getReservationDatetime());
        reservation.setReservationDatetime(reservationTimestamp);
        reservation.setNumberOfPeople(registerForm.getNumberOfPeople());
        reservation.setStore(store);

        // Memberエンティティを取得し、Reservationに設定
        Member member = memberRepository.findById(registerForm.getMemberId())
                          .orElseThrow(() -> new IllegalArgumentException("Invalid member ID: " + registerForm.getMemberId()));
        reservation.setMember(member);

        // Reservationを保存
        reservationRepository.save(reservation);

        // 予約一覧ページにリダイレクト
        return "redirect:/reservations";
    }
    
    // 予約をキャンセル
    @PostMapping("/reservations/cancel/{id}")
    @ResponseBody
    public ResponseEntity<?> cancelReservation(@PathVariable Long id) {
        try {
            boolean isCancelled = reservationRepository.existsById(id);
            if (isCancelled) {
                reservationRepository.deleteById(id);
                return ResponseEntity.ok("予約をキャンセルしました。");
            } else {
                return ResponseEntity.status(404).body("予約が見つかりません。");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("予約キャンセル処理に失敗しました。");
        }
    }
    
 // レビュー削除
    @PostMapping("/reviews/delete/{id}")
    @ResponseBody
    public ResponseEntity<String> deleteReview(@PathVariable("id") Long id) {
        try {
            if (reviewRepository.existsById(id)) {
                reviewRepository.deleteById(id);
                return ResponseEntity.ok("レビューを削除しました。");
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("レビューが見つかりません。");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("削除に失敗しました。");
        }
    }
}