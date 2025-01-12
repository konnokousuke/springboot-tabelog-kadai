package com.example.nagoyameshi.controller;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.nagoyameshi.entity.Category;
import com.example.nagoyameshi.entity.Review;
import com.example.nagoyameshi.entity.Store;
import com.example.nagoyameshi.form.StoreEditForm;
import com.example.nagoyameshi.form.StoreRegisterForm;
import com.example.nagoyameshi.repository.CategoryRepository;
import com.example.nagoyameshi.repository.ReviewRepository;
import com.example.nagoyameshi.repository.StoreRepository;
import com.example.nagoyameshi.service.StoreService;

@Controller
@RequestMapping("/admin/stores")
public class AdminStoreController {
	private final StoreRepository storeRepository;
	private final StoreService storeService;
	private final ReviewRepository reviewRepository;
	private final CategoryRepository categoryRepository;

	public AdminStoreController(StoreRepository storeRepository, StoreService storeService, ReviewRepository reviewRepository, CategoryRepository categoryRepository) {
		this.storeRepository = storeRepository;
		this.storeService = storeService;
		this.reviewRepository = reviewRepository;
		this.categoryRepository = categoryRepository;
	}

	@GetMapping
	public String index(Model model, 
	                    @PageableDefault(page = 0, size = 10, sort = "storeId", direction = Direction.ASC) Pageable pageable, 
	                    @RequestParam(name = "keyword", required = false) String keyword, 
	                    @ModelAttribute("successMessage") String successMessage) {
	    Page<Store> storePage;

	    if (keyword != null && !keyword.isEmpty()) {
	        storePage = storeRepository.findByStoreNameLike("%" + keyword + "%", pageable);
	    } else {
	        storePage = storeRepository.findAll(pageable);
	    }

	    // 各店舗の平均評価を計算してマッピング
	    Map<Long, String> averageRatings = new HashMap<>();
	    for (Store store : storePage.getContent()) {
	        String averageRating = storeService.calculateAverageRating(store.getStoreId());
	        averageRatings.put(store.getStoreId(), averageRating);
	    }

	    model.addAttribute("storePage", storePage);
	    model.addAttribute("averageRatings", averageRatings); // 平均評価をテンプレートに渡す
	    model.addAttribute("keyword", keyword);
	    model.addAttribute("successMessage", successMessage);

	    return "admin/stores/index";
	}

	@GetMapping("/{id}")
	public String show(@PathVariable(name = "id") Long id, Model model) {
	    Optional<Store> storeOptional = storeRepository.findById(id);
	    if (storeOptional.isEmpty()) {
	        model.addAttribute("errorMessage", "指定された店舗が見つかりません。");
	        return "admin/stores/error";
	    }

	    Store store = storeOptional.get();
	    model.addAttribute("store", store);

	    // レビューを取得してテンプレートに渡す
	    List<Review> reviews = reviewRepository.findByStoreId(store.getStoreId());
	    model.addAttribute("reviews", reviews);

	    // StoreServiceを使ってレビューの平均値を計算
	    String averageRatingFormatted = storeService.calculateAverageRating(store.getStoreId());
	    model.addAttribute("averageRatingFormatted", averageRatingFormatted);

	    return "admin/stores/show";
	}

	@GetMapping("/register")
	public String register(Model model) {
		model.addAttribute("storeRegisterForm", new StoreRegisterForm());
		model.addAttribute("categories", categoryRepository.findAll());
		model.addAttribute("daysOfWeek", DayOfWeek.values()); // 曜日データを渡す
		return "admin/stores/register";
	}

	@PostMapping("/create")
	public String create(@ModelAttribute @Validated StoreRegisterForm storeRegisterForm, 
	                     BindingResult bindingResult, 
	                     Model model, 
	                     RedirectAttributes redirectAttributes) {
	    if (bindingResult.hasErrors()) {
	        System.out.println("DEBUG: Validation errors detected");
	        bindingResult.getAllErrors().forEach(error -> System.out.println("Validation error: " + error.getDefaultMessage()));
	        model.addAttribute("categories", categoryRepository.findAll());
	        return "admin/stores/register";
	    }

	    try {
	        storeService.create(storeRegisterForm);
	        redirectAttributes.addFlashAttribute("successMessage", "店舗の登録が完了しました。");
	        return "redirect:/admin/stores";
	    } catch (Exception e) {
	        model.addAttribute("errorMessage", "登録処理中にエラーが発生しました: " + e.getMessage());
	        model.addAttribute("categories", categoryRepository.findAll());
	        return "admin/stores/register";
	    }
	}

	@GetMapping("/{id}/edit")
	public String edit(@PathVariable(name = "id") Long id, Model model) {
	    try {
	        Store store = storeRepository.getReferenceById(id);

	        StoreEditForm storeEditForm = new StoreEditForm(
	            store.getStoreId(),
	            store.getStoreName(),
	            null, // 画像フィールドはnullのまま
	            store.getDescription(),
	            Long.valueOf(store.getPrice()),
	            store.getPostalCode(),
	            store.getAddress(),
	            store.getPhoneNumber(),
	            store.getOpeningHours(),
	            store.getClosingTime() != null ? store.getClosingTime().toString() : null,
	            store.getClosedDays(),
	            store.getCategories().stream().map(Category::getCategoryId).collect(Collectors.toList())
	        );

	        model.addAttribute("storeEditForm", storeEditForm);
	        model.addAttribute("categories", categoryRepository.findAll()); // カテゴリデータを渡す
	        model.addAttribute("daysOfWeek", List.of("日", "月", "火", "水", "木", "金", "土")); // 日本語曜日を渡す

	        return "admin/stores/edit";
	    } catch (Exception e) {
	        model.addAttribute("errorMessage", "店舗情報の取得に失敗しました。");
	        return "error";
	    }
	}

	@PostMapping("/{id}/update")
	public String update(
	        @ModelAttribute @Validated StoreEditForm storeEditForm,
	        BindingResult bindingResult,
	        Model model,
	        RedirectAttributes redirectAttributes) {

	    if (bindingResult.hasErrors()) {
	        // エラー時にカテゴリと曜日データを再度セット
	        model.addAttribute("categories", categoryRepository.findAll());
	        model.addAttribute("daysOfWeek", List.of("日", "月", "火", "水", "木", "金", "土")); // 日本語曜日を渡す
	        return "admin/stores/edit";
	    }

	    try {
	        // サービスを通じて更新処理を実行
	        storeService.update(storeEditForm);
	        redirectAttributes.addFlashAttribute("successMessage", "店舗情報を更新しました。");
	        return "redirect:/admin/stores";
	    } catch (Exception e) {
	        // 更新処理中にエラーが発生した場合
	        model.addAttribute("errorMessage", "更新処理中にエラーが発生しました: " + e.getMessage());
	        model.addAttribute("categories", categoryRepository.findAll());
	        model.addAttribute("daysOfWeek", List.of("日", "月", "火", "水", "木", "金", "土"));
	        return "admin/stores/edit";
	    }
	}

	@PostMapping("/{id}/delete")
	public String delete(@PathVariable(name = "id") Long id, RedirectAttributes redirectAttributes) {
	    try {
	        storeService.deleteStoreWithDependencies(id);
	        redirectAttributes.addFlashAttribute("successMessage", "店舗の削除が完了しました。");
	    } catch (Exception e) {
	        redirectAttributes.addFlashAttribute("errorMessage", "削除処理中にエラーが発生しました: " + e.getMessage());
	    }
	    return "redirect:/admin/stores";
	}
	
	@GetMapping("/storage/{filename}")
	public ResponseEntity<Resource> getImage(@PathVariable String filename) throws MalformedURLException {
	    Path filePath = Paths.get("src/main/resources/static/storage/" + filename);
	    Resource resource = new UrlResource(filePath.toUri());
	    return ResponseEntity.ok()
	            .contentType(MediaType.IMAGE_JPEG) // 必要に応じて変更
	            .body(resource);
	}

	// レビューの平均値を計算してテンプレートに渡す
	@GetMapping("/admin/stores/{id}")
	public String showAdminStoreDetails(@PathVariable(name = "id") Long id, Model model) {
	    Optional<Store> storeOptional = storeRepository.findById(id);
	    if (storeOptional.isPresent()) {
	        Store store = storeOptional.get();
	        model.addAttribute("store", store);

	        List<Review> reviews = reviewRepository.findByStoreId(store.getStoreId());
	        model.addAttribute("reviews", reviews);

	        double averageRating = reviews.stream()
	                                      .mapToInt(Review::getRating)
	                                      .average()
	                                      .orElse(0.0);

	        String formattedAverageRating = averageRating > 0 ? String.format("%.1f", averageRating) : "N/A";
	        model.addAttribute("averageRatingFormatted", formattedAverageRating);
	    } else {
	        model.addAttribute("errorMessage", "指定された店舗が見つかりません。");
	        return "admin/stores/error";
	    }

	    return "admin/stores/show";
	}
}
