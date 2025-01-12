package com.example.nagoyameshi.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.example.nagoyameshi.entity.Favorite;
import com.example.nagoyameshi.entity.Member;
import com.example.nagoyameshi.entity.Store;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    boolean existsByMemberAndStore(Member member, Store store);

    void deleteByMemberAndStore(Member member, Store store);

    Page<Favorite> findByMember(Member member, Pageable pageable);
    
    // 店舗削除用
    @Modifying
    @Transactional
    @Query("DELETE FROM Favorite f WHERE f.store.id = :storeId")
    void deleteByStoreId(@Param("storeId") Long storeId);

}