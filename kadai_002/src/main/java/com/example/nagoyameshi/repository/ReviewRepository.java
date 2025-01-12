package com.example.nagoyameshi.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.example.nagoyameshi.entity.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    @Query("SELECT r FROM Review r WHERE r.store.storeId = :storeId")
    List<Review> findByStoreId(@Param("storeId") Long storeId);

    @Query("SELECT r FROM Review r WHERE r.store.storeId = :storeId")
    Page<Review> findPagedByStoreId(@Param("storeId") Long storeId, Pageable pageable);
    
    // ユーザーと店舗に基づいてレビューを検索
    @Query("SELECT r FROM Review r WHERE r.member.id = :memberId AND r.store.storeId = :storeId")
    Optional<Review> findByMemberIdAndStoreId(Integer memberId, Long storeId);
    
    Page<Review> findByMemberId(Integer memberId, Pageable pageable);
    
    // 店舗削除用
    @Modifying
    @Transactional
    @Query("DELETE FROM Review r WHERE r.store.storeId = :storeId")
    void deleteByStoreId(Long storeId);
    
    // レビュー削除用
    @Modifying
    @Transactional
    @Query("DELETE FROM Review r WHERE r.reviewId = :reviewId")
    void deleteByReviewId(@Param("reviewId") Long reviewId);
    
}