package com.example.nagoyameshi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.nagoyameshi.entity.Category;
import com.example.nagoyameshi.repository.CategoryRepository;
import com.example.nagoyameshi.repository.StoreCategoryRepository;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private StoreCategoryRepository storeCategoryRepository;

    public Page<Category> searchCategories(String keyword, Pageable pageable) {
        if (keyword == null || keyword.isEmpty()) {
            return categoryRepository.findAll(pageable);
        } else {
            return categoryRepository.findByNameContaining(keyword, pageable);
        }
    }

    public void updateCategoryName(Long id, String name) {
        Category category = categoryRepository.findById(id).orElseThrow(() -> new RuntimeException("カテゴリが見つかりません"));
        category.setName(name);
        categoryRepository.save(category);
    }
    
    // カテゴリ削除用
    @Transactional
    public String deleteCategory(Long categoryId) {
        Logger logger = LoggerFactory.getLogger(this.getClass());
        
        // カテゴリに関連する店舗が存在するかを確認
        boolean hasRelatedStores = storeCategoryRepository.existsByIdCategoryId(categoryId);
        logger.info("カテゴリID {} に関連する店舗が存在するか: {}", categoryId, hasRelatedStores);
        
        if (hasRelatedStores) {
            logger.warn("カテゴリID {} は関連する店舗が存在するため削除できません", categoryId);
            return "このカテゴリを使用している店舗があるため削除できません";
        }

        // 関連データを削除
        storeCategoryRepository.deleteByCategoryId(categoryId);
        logger.info("store_category テーブルからカテゴリID {} を削除しました", categoryId);

        // カテゴリを削除
        categoryRepository.deleteById(categoryId);
        logger.info("categories テーブルからカテゴリID {} を削除しました", categoryId);

        return null; // エラーがない場合はnullを返す
    }
    
    // カテゴリ追加用
    @Transactional
    public void createCategory(Category category) {
        categoryRepository.save(category);
    }
}
