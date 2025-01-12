package com.example.nagoyameshi.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.nagoyameshi.entity.Store;

public interface StoreRepository extends JpaRepository<Store, Long> {
	public Page<Store> findByStoreNameLike(String keyword, Pageable pageable);
	
	public Page<Store> findByStoreNameLikeOrDescriptionLikeOrderByCreatedAtDesc(String storeNameKeword, String descriptionKeyword, Pageable pageable);
	public Page<Store> findByStoreNameLikeOrDescriptionLikeOrderByPriceAsc(String storeNameKeword, String descriptionKeyword, Pageable pageable);
	public Page<Store> findByPriceLessThanEqualOrderByCreatedAtDesc(Integer price, Pageable pageable);
	public Page<Store> findByPriceLessThanEqualOrderByPriceAsc(Integer price, Pageable pageable);
	public Page<Store> findAllByOrderByCreatedAtDesc(Pageable pageable);
	public Page<Store> findAllByOrderByPriceAsc(Pageable pageable);
	
	public List<Store> findTop10ByOrderByCreatedAtDesc();
	
	// カテゴリ検索用
	Page<Store> findByCategories_CategoryId(Long categoryId, Pageable pageable);
	
	// レビュー並べ替え用(降順)
	@Query("SELECT s FROM Store s LEFT JOIN Review r ON s.storeId = r.store.storeId " +
		       "GROUP BY s.storeId ORDER BY AVG(r.rating) DESC")
		Page<Store> findAllOrderByRatingDesc(Pageable pageable);
	
	// レビュー並べ替え用(昇順)
	@Query("SELECT s FROM Store s LEFT JOIN Review r ON s.storeId = r.store.storeId " +
		       "GROUP BY s.storeId ORDER BY AVG(r.rating) ASC")
		Page<Store> findAllOrderByRatingAsc(Pageable pageable);
	
	// 重複確認用
	@Query("SELECT COUNT(sc) > 0 " +
		       "FROM Store s JOIN s.categories sc " +
		       "WHERE s.storeId = :storeId AND sc.categoryId = :categoryId")
		boolean existsStoreCategory(@Param("storeId") Long storeId, @Param("categoryId") Long categoryId);
}
