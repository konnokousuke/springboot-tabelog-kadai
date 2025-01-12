package com.example.nagoyameshi.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.nagoyameshi.entity.Category;
import com.example.nagoyameshi.service.CategoryService;

@Controller
@RequestMapping("/admin/categories")
public class AdminCategoryController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping
    public String showCategories(@RequestParam(required = false) String keyword,
                                 @RequestParam(defaultValue = "1") int page,
                                 Model model) {
        int pageSize = 10; // 1ページあたりのカテゴリ数
        Pageable pageable = PageRequest.of(page - 1, pageSize);
        Page<Category> categoryPage = categoryService.searchCategories(keyword, pageable);

        model.addAttribute("categoryPage", categoryPage); // categoryPage全体を渡す
        model.addAttribute("keyword", keyword);

        return "admin/categories/index";
    }
    
    // カテゴリ更新用
    @PostMapping("/update")
    public String updateCategory(@RequestParam Long id, @RequestParam String name, RedirectAttributes redirectAttributes) {
        categoryService.updateCategoryName(id, name);
        redirectAttributes.addFlashAttribute("successMessage", "カテゴリ名を更新しました。");
        return "redirect:/admin/categories";
    }

    // カテゴリ削除用
    @PostMapping("/delete")
    public String deleteCategory(@RequestParam Long id, RedirectAttributes redirectAttributes) {
        String errorMessage = categoryService.deleteCategory(id);
        if (errorMessage != null) {
            redirectAttributes.addFlashAttribute("errorMessage", errorMessage);
        } else {
        	redirectAttributes.addFlashAttribute("successMessage", "カテゴリを削除しました。");
        }
        return "redirect:/admin/categories";
    }
    
    // カテゴリ追加用
    @GetMapping("/create")
    public String showCreateCategoryForm(Model model) {
        model.addAttribute("category", new Category()); // 新規カテゴリオブジェクトを渡す
        return "admin/categories/create";
    }

    @PostMapping("/create")
    public String createCategory(Category category, RedirectAttributes redirectAttributes) {
        categoryService.createCategory(category);
        redirectAttributes.addFlashAttribute("successMessage", "カテゴリを追加しました。");
        return "redirect:/admin/categories";
    }
}
