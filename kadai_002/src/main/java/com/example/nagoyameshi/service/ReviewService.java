package com.example.nagoyameshi.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.nagoyameshi.dto.ReviewDTO;
import com.example.nagoyameshi.entity.Member;
import com.example.nagoyameshi.entity.Review;
import com.example.nagoyameshi.entity.Store;
import com.example.nagoyameshi.form.ReviewForm;
import com.example.nagoyameshi.repository.ReviewRepository;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;

    public ReviewService(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    // レビューの保存
    public void saveReview(ReviewForm form, Store store, Member member) {
    	// 既存のレビューを検索
    	Optional<Review> existingReview = reviewRepository.findByMemberIdAndStoreId(member.getId(), store.getStoreId());
    	Review review;
        if (existingReview.isPresent()) {
            // 既存レビューを更新
            review = existingReview.get();
            review.setRating(form.getRating());
            review.setComment(form.getComment());
        } else {
            // 新しいレビューを作成
            review = new Review();
            review.setStore(store);
            review.setMember(member);
            review.setRating(form.getRating());
            review.setComment(form.getComment());
        }

        // レビューを保存（新規または更新）
        reviewRepository.save(review);
    }

    // レビュー一覧の取得
    public List<ReviewDTO> getReviewsByStore(Store store) {
        return reviewRepository.findByStoreId(store.getStoreId())
                .stream()
                .map(review -> new ReviewDTO(
                        review.getReviewId(),
                        store.getStoreId(),
                        review.getMember().getName(),
                        review.getRating(),
                        review.getComment(),
                        review.getCreatedAt()))
                .collect(Collectors.toList());
    }
    
    // 過去レビューを検索する
    public Optional<Review> findReviewByMemberAndStore(Integer memberId, Long storeId) {
        return reviewRepository.findByMemberIdAndStoreId(memberId, storeId);
    }
    
    // ページネーションでレビューを取得する
    public Page<Review> getReviewsByMemberId(Integer memberId, Pageable pageable) {
        return reviewRepository.findByMemberId(memberId, pageable);
    }

}