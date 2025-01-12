package com.example.nagoyameshi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.nagoyameshi.entity.StoreCategory;
import com.example.nagoyameshi.entity.StoreCategoryId;

public interface StoreCategoryRepository extends JpaRepository<StoreCategory, StoreCategoryId> {

    // カテゴリIDに関連するデータの存在確認
    @Query("SELECT COUNT(sc) > 0 FROM StoreCategory sc WHERE sc.id.categoryId = :categoryId")
    boolean existsByIdCategoryId(@Param("categoryId") Long categoryId); // 修正済み

    @Modifying
    @Query("DELETE FROM StoreCategory sc WHERE sc.id.categoryId = :categoryId")
    void deleteByCategoryId(@Param("categoryId") Long categoryId);

    @Modifying
    @Query("DELETE FROM StoreCategory sc WHERE sc.id.storeId = :storeId")
    void deleteByStoreId(@Param("storeId") Long storeId);
}
