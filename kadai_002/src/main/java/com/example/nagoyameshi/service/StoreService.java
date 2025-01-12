package com.example.nagoyameshi.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.nagoyameshi.entity.Category;
import com.example.nagoyameshi.entity.Review;
import com.example.nagoyameshi.entity.Store;
import com.example.nagoyameshi.form.StoreEditForm;
import com.example.nagoyameshi.form.StoreRegisterForm;
import com.example.nagoyameshi.repository.CategoryRepository;
import com.example.nagoyameshi.repository.FavoriteRepository;
import com.example.nagoyameshi.repository.ReservationRepository;
import com.example.nagoyameshi.repository.ReviewRepository;
import com.example.nagoyameshi.repository.StoreCategoryRepository;
import com.example.nagoyameshi.repository.StoreRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class StoreService {

	@PersistenceContext
    private EntityManager entityManager;

	private final StoreRepository storeRepository;
	private final ReviewRepository reviewRepository;
	private final CategoryRepository categoryRepository;
	private final StoreCategoryRepository storeCategoryRepository;
	private final ReservationRepository reservationRepository;
	private final FavoriteRepository favoriteRepository;

	public StoreService(StoreRepository storeRepository, ReviewRepository reviewRepository, CategoryRepository categoryRepository, StoreCategoryRepository storeCategoryRepository, ReservationRepository reservationRepository, FavoriteRepository favoriteRepository) {
		this.storeRepository = storeRepository;
		this.reviewRepository = reviewRepository;
		this.categoryRepository = categoryRepository;
		this.storeCategoryRepository = storeCategoryRepository;
		this.reservationRepository = reservationRepository;
		this.favoriteRepository = favoriteRepository;
	}

	@Transactional
	public void create(StoreRegisterForm storeRegisterForm) {
	    Store store = new Store();
	    MultipartFile imageFile = storeRegisterForm.getImageFile();

	    // 画像ファイルの保存
	    if (imageFile != null && !imageFile.isEmpty()) {
	        try {
	            String hashedImageName = generateNewFileName(imageFile.getOriginalFilename());
	            Path filePath = Paths.get("src/main/resources/static/storage/" + hashedImageName);
	            createStorageDirectoryIfNotExists(filePath.getParent());
	            copyImageFile(imageFile, filePath);
	            store.setImageFilename(hashedImageName);
	        } catch (IOException e) {
	            throw new RuntimeException("画像の保存中にエラーが発生しました: " + e.getMessage(), e);
	        }
	    }
	    // 基本情報の設定
	    store.setStoreName(storeRegisterForm.getName());
	    store.setDescription(storeRegisterForm.getDescription());
	    store.setPrice(storeRegisterForm.getPrice() != null ? storeRegisterForm.getPrice().intValue() : 0);
	    store.setPostalCode(storeRegisterForm.getPostalCode());
	    store.setAddress(storeRegisterForm.getAddress());
	    store.setPhoneNumber(storeRegisterForm.getPhoneNumber());
	    store.setOpeningHours(storeRegisterForm.getOpeningHours());

	    // 営業時間を生成し保存
	    String openingHours = storeRegisterForm.getOpeningHours() + " - " + storeRegisterForm.getClosingTime();
	    store.setOpeningHours(openingHours);

	    // LocalTime型に変換して設定
	    try {
	        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
	        LocalTime closingTime = LocalTime.parse(storeRegisterForm.getClosingTime(), timeFormatter);
	        store.setClosingTime(closingTime);
	    } catch (DateTimeParseException e) {
	        throw new RuntimeException("営業時間（終了）のフォーマットが不正です: " + e.getMessage(), e);
	    }

	    // カテゴリの設定
	    Long categoryId = storeRegisterForm.getCategories().get(0); // 単一選択として最初のカテゴリを使用

	    // 重複チェック
	    if (storeRepository.existsStoreCategory(store.getStoreId(), categoryId)) {
	        throw new RuntimeException("このカテゴリは既に店舗に紐付けられています: Store ID = " 
	                                   + store.getStoreId() + ", Category ID = " + categoryId);
	    }

	    Category category = categoryRepository.findById(categoryId)
	            .orElseThrow(() -> new RuntimeException("カテゴリが見つかりません: ID = " + categoryId));
	    store.setCategories(Set.of(category)); // 単一カテゴリを設定

	    // 定休日の設定
	    List<String> dayOffList = storeRegisterForm.getDayOff();
	    if (dayOffList.contains("なし")) {
	        store.setClosedDays("なし"); // 「なし」を保存
	    } else {
	        String closedDays = String.join(",", dayOffList);
	        store.setClosedDays(closedDays);
	    }
	    
	    // 店舗データを保存
	    storeRepository.save(store);
	    storeRepository.flush(); // Hibernateのキャッシュをクリア
	    System.out.println("DEBUG: Store saved with ID = " + store.getStoreId());
	}

	@Transactional
	public void addCategoryToStore(Long storeId, Long categoryId) {
	    Store store = storeRepository.findById(storeId)
	        .orElseThrow(() -> new RuntimeException("店舗が見つかりません: ID = " + storeId));
	    Category category = categoryRepository.findById(categoryId)
	        .orElseThrow(() -> new RuntimeException("カテゴリが見つかりません: ID = " + categoryId));

	    // 重複チェックを追加
	    if (store.getCategories().stream().anyMatch(c -> c.getCategoryId().equals(categoryId))) {
	        System.out.println("カテゴリは既に登録されています: " + category.getName());
	        return; // 重複している場合は処理を終了
	    }

	    // カテゴリの追加
	    store.getCategories().add(category);
	    storeRepository.save(store);
	    System.out.println("カテゴリを追加しました: " + category.getName());
	}

	@Transactional
	public void update(StoreEditForm storeEditForm) {
	    Store store = storeRepository.getReferenceById(storeEditForm.getStoreId());
	    MultipartFile imageFile = storeEditForm.getImageFile();

	    // 画像ファイルの保存と既存ファイルの削除
	    if (imageFile != null && !imageFile.isEmpty()) {
	        try {
	            if (store.getImageFilename() != null) {
	                Path oldFilePath = Paths.get("src/main/resources/static/storage/" + store.getImageFilename());
	                Files.deleteIfExists(oldFilePath);
	                System.out.println("DEBUG: 古い画像ファイルを削除しました: " + oldFilePath);
	            }

	            String hashedImageName = generateNewFileName(imageFile.getOriginalFilename());
	            Path filePath = Paths.get("src/main/resources/static/storage/" + hashedImageName);
	            createStorageDirectoryIfNotExists(filePath.getParent());
	            copyImageFile(imageFile, filePath);
	            store.setImageFilename(hashedImageName);
	            System.out.println("DEBUG: 新しい画像ファイルを保存しました: " + filePath);
	        } catch (IOException e) {
	            throw new RuntimeException("画像の保存中にエラーが発生しました: " + e.getMessage(), e);
	        }
	    } else {
	        // 画像未選択の場合、既存の画像を保持
	        System.out.println("DEBUG: 画像未選択のため既存の画像を保持します: " + store.getImageFilename());
	    }

	    // 基本情報の更新
	    store.setStoreName(storeEditForm.getName());
	    store.setDescription(storeEditForm.getDescription());
	    store.setPrice(storeEditForm.getPrice() != null ? storeEditForm.getPrice().intValue() : 0);
	    store.setPostalCode(storeEditForm.getPostalCode());
	    store.setAddress(storeEditForm.getAddress());
	    store.setPhoneNumber(storeEditForm.getPhoneNumber());

	    // 営業時間の更新
	    try {
	        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
	        LocalTime closingTime = LocalTime.parse(storeEditForm.getClosingTime(), timeFormatter);
	        store.setOpeningHours(storeEditForm.getOpeningHours() + " - " + storeEditForm.getClosingTime());
	        store.setClosingTime(closingTime);
	    } catch (DateTimeParseException e) {
	        throw new RuntimeException("営業時間（終了）のフォーマットが不正です: " + e.getMessage(), e);
	    }

	    // カテゴリの更新 - 既存データの削除
	    storeCategoryRepository.deleteByStoreId(store.getStoreId());

	    // 新しいカテゴリを追加
	    if (storeEditForm.getCategories() != null && !storeEditForm.getCategories().isEmpty()) {
	        List<Category> newCategories = categoryRepository.findAllById(storeEditForm.getCategories());
	        for (Category category : newCategories) {
	            store.getCategories().add(category); // カテゴリを再設定
	        }
	    }

	 // 定休日の更新
	    List<String> dayOffList = storeEditForm.getDayOff();
	    if (dayOffList.contains("なし")) {
	        store.setClosedDays("なし"); //「なし」を保存
	    } else {
	        String closedDays = String.join(",", dayOffList);
	        store.setClosedDays(closedDays);
	    }
	    
	    // 最終保存
	    storeRepository.save(store);
	    storeRepository.flush(); // Hibernate キャッシュのクリア
	    System.out.println("DEBUG: 店舗情報を更新しました: " + store.getStoreId());
	}

	private void deleteStoreCategories(Long storeId) {
	    String sql = "DELETE FROM store_category WHERE store_id = :storeId";
	    entityManager.createNativeQuery(sql)
	                 .setParameter("storeId", storeId)
	                 .executeUpdate();
	}

	public void copyImageFile(MultipartFile imageFile, Path filePath) throws IOException {
	    Files.copy(imageFile.getInputStream(), filePath);
	}

	private void createStorageDirectoryIfNotExists(Path directory) throws IOException {
	    if (!Files.exists(directory)) {
	        Files.createDirectories(directory);
	        System.out.println("DEBUG: ディレクトリを作成しました: " + directory);
	    } else {
	        System.out.println("DEBUG: ディレクトリは既に存在します: " + directory);
	    }
	}

	public String generateNewFileName(String originalFileName) {
	    String extension = "";
	    int index = originalFileName.lastIndexOf(".");
	    if (index > 0) {
	        extension = originalFileName.substring(index);
	    }
	    return UUID.randomUUID().toString() + extension;
	}

	// カテゴリ検索用
	 public Page<Store> findByCategory(Long categoryId, Pageable pageable) {
	        return storeRepository.findByCategories_CategoryId(categoryId, pageable);
	    }

	 // レビュー並べ替え用
	 public Page<Store> getStoresSortedByRating(String sortType, Pageable pageable) {
	        if ("reviewHigh".equals(sortType)) {
	            return storeRepository.findAllOrderByRatingDesc(pageable);
	        } else if ("reviewLow".equals(sortType)) {
	            return storeRepository.findAllOrderByRatingAsc(pageable);
	        } else {
	            return storeRepository.findAll(pageable); // デフォルトのソート
	        }
	    }

	// レビューの平均値を計算する
	    public String calculateAverageRating(Long storeId) {
	        List<Review> reviews = reviewRepository.findByStoreId(storeId);
	        double averageRating = reviews.stream()
	                                      .mapToInt(Review::getRating)
	                                      .average()
	                                      .orElse(0.0);

	        return averageRating > 0 ? String.format("%.1f", averageRating) : "N/A";
	    }

	    // 店舗削除用
	    @Transactional
	    public void deleteStoreWithDependencies(Long storeId) {
	        try {
	            System.out.println("DEBUG: 店舗ID " + storeId + " の関連データを削除開始");

	            // お気に入り情報を削除
	            favoriteRepository.deleteByStoreId(storeId);
	            System.out.println("DEBUG: お気に入り情報を削除しました");

	            // レビュー情報を削除
	            reviewRepository.deleteByStoreId(storeId);
	            System.out.println("DEBUG: レビュー情報を削除しました");

	            // カテゴリ情報を削除
	            storeCategoryRepository.deleteByStoreId(storeId);
	            System.out.println("DEBUG: カテゴリ情報を削除しました");

	            // 予約データを削除
	            reservationRepository.deleteByStoreId(storeId);
	            System.out.println("DEBUG: 予約情報を削除しました");

	            // 最後に店舗データを削除
	            storeRepository.deleteById(storeId);
	            System.out.println("DEBUG: 店舗を削除しました");
	        } catch (Exception e) {
	            throw new RuntimeException("店舗と関連データの削除中にエラーが発生: " + e.getMessage(), e);
	        }
	    }
}