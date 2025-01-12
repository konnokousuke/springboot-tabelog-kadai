package com.example.nagoyameshi.controller;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.nagoyameshi.entity.Member;
import com.example.nagoyameshi.entity.Review;
import com.example.nagoyameshi.entity.Store;
import com.example.nagoyameshi.form.ReviewForm;
import com.example.nagoyameshi.repository.ReviewRepository;
import com.example.nagoyameshi.repository.StoreRepository;
import com.example.nagoyameshi.security.MemberDetailsImpl;
import com.example.nagoyameshi.service.ReviewService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/stores/{storeId}/reviews")
public class ReviewController {

    private static final Logger logger = LoggerFactory.getLogger(ReviewController.class);

    private final ReviewService reviewService;
    private final StoreRepository storeRepository;
    private final ReviewRepository reviewRepository;

    public ReviewController(ReviewService reviewService, StoreRepository storeRepository, ReviewRepository reviewRepository) {
        this.reviewService = reviewService;
        this.storeRepository = storeRepository;
        this.reviewRepository = reviewRepository;
    }

 // レビュー投稿フォームの表示
    @GetMapping("/new")
    public String showReviewForm(@PathVariable Long storeId, Model model) {
        Optional<Store> storeOptional = storeRepository.findById(storeId);
        if (storeOptional.isEmpty()) {
            model.addAttribute("errorMessage", "店舗が見つかりません");
            return "error/404"; 
        }
        Store store = storeOptional.get();

        // 現在のログインユーザーを取得
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            model.addAttribute("errorMessage", "ログインが必要です。");
            return "error/403";
        }
        MemberDetailsImpl memberDetails = (MemberDetailsImpl) authentication.getPrincipal();
        Member member = memberDetails.getMember();

        // 過去のレビューを取得
        Optional<Review> existingReview = reviewService.findReviewByMemberAndStore(member.getId(), store.getStoreId());

        // フォームに過去のレビュー情報を設定
        ReviewForm reviewForm = new ReviewForm();
        if (existingReview.isPresent()) {
            Review review = existingReview.get();
            reviewForm.setRating(review.getRating());
            reviewForm.setComment(review.getComment());
        }

        model.addAttribute("store", store);
        model.addAttribute("reviewForm", reviewForm);

        return "reviews/post";
    }

    // レビューの保存
    @PostMapping
    public String submitReview(
            @PathVariable Long storeId,
            @Valid @ModelAttribute("reviewForm") ReviewForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        logger.debug("レビュー投稿リクエストを受信: storeId={}, form={}", storeId, form);

        // 認証されたユーザーを取得
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            logger.warn("未認証のユーザーがレビュー投稿を試みました。");
            model.addAttribute("errorMessage", "ログインが必要です。");
            return "error/403";
        }

        // `MemberDetailsImpl` を取得し、`Member` を取得
        MemberDetailsImpl memberDetails = (MemberDetailsImpl) authentication.getPrincipal();
        Member member = memberDetails.getMember();
        logger.debug("認証済みユーザー: {}, 有料会員: {}", member.getEmail(), memberDetails.isPaidMember());

        // 店舗の存在確認
        Optional<Store> storeOptional = storeRepository.findById(storeId);
        if (storeOptional.isEmpty()) {
            logger.warn("無効な店舗IDが指定されました: storeId={}", storeId);
            model.addAttribute("errorMessage", "店舗が見つかりません");
            return "error/404";
        }
        Store store = storeOptional.get();

        logger.debug("対象店舗情報: {}", store);

        // バリデーションエラーのチェック
        if (bindingResult.hasErrors()) {
            logger.debug("フォームバリデーションエラー: {}", bindingResult.getAllErrors());
            model.addAttribute("store", store);
            model.addAttribute("reviewForm", form);
            return "reviews/post";
        }

        try {
            // レビューの保存
            logger.debug("レビュー保存を開始: member={}, store={}, form={}", member, store, form);
            reviewService.saveReview(form, store, member);
            logger.info("レビューが正常に保存されました: member={}, storeId={}", member.getId(), storeId);
            redirectAttributes.addFlashAttribute("successMessage", "レビューを投稿しました。");
            return "redirect:/stores/" + storeId;

        } catch (DataIntegrityViolationException e) {
            // UNIQUE制約などのデータベースエラーの場合
            logger.error("データベースエラーが発生しました: ", e);
            model.addAttribute("errorMessage", "同じ店舗へのレビューは1回のみ投稿可能です。");
            model.addAttribute("store", store);
            model.addAttribute("reviewForm", form);
            return "reviews/post";

        } catch (Exception e) {
            // その他予期しないエラーの場合
            logger.error("予期しないエラーが発生しました: ", e);
            model.addAttribute("errorMessage", "予期しないエラーが発生しました。");
            return "error";
        }
    }
    
    // レビュー編集
    @GetMapping("/post/{reviewId}")
    public String editReview(
            @PathVariable Long storeId,
            @PathVariable Long reviewId,
            Model model) {
        // 店舗を取得
        Optional<Store> storeOptional = storeRepository.findById(storeId);
        if (storeOptional.isEmpty()) {
            model.addAttribute("errorMessage", "店舗が見つかりません");
            return "error/404";
        }
        Store store = storeOptional.get();

        // レビューを取得
        Optional<Review> reviewOptional = reviewRepository.findById(reviewId);
        if (reviewOptional.isEmpty()) {
            model.addAttribute("errorMessage", "レビューが見つかりません");
            return "error/404";
        }
        Review review = reviewOptional.get();

        // フォームにレビュー情報を設定
        ReviewForm reviewForm = new ReviewForm();
        reviewForm.setRating(review.getRating());
        reviewForm.setComment(review.getComment());

        model.addAttribute("store", store);
        model.addAttribute("reviewForm", reviewForm);
        model.addAttribute("editMode", true); // 編集モードフラグを設定

        return "reviews/post";
    }

 // レビュー削除用
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