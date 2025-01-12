package com.example.nagoyameshi.controller;

import java.security.Principal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.nagoyameshi.entity.Category;
import com.example.nagoyameshi.entity.Reservation;
import com.example.nagoyameshi.entity.Review;
import com.example.nagoyameshi.entity.Store;
import com.example.nagoyameshi.form.ReservationInputForm;
import com.example.nagoyameshi.repository.CategoryRepository;
import com.example.nagoyameshi.repository.ReviewRepository;
import com.example.nagoyameshi.repository.StoreRepository;
import com.example.nagoyameshi.security.MemberDetailsImpl;
import com.example.nagoyameshi.service.FavoriteService;
import com.example.nagoyameshi.service.ReservationService;
import com.example.nagoyameshi.service.StoreService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/stores")
public class StoreController {

    private final StoreRepository storeRepository;
    private final ReservationService reservationService;
    private final FavoriteService favoriteService;
    private final ReviewRepository reviewRepository;
    private final CategoryRepository categoryRepository;
    private final StoreService storeService;

    // コンストラクタで依存性を注入
    public StoreController(StoreRepository storeRepository, ReservationService reservationService, FavoriteService favoriteService, ReviewRepository reviewRepository, CategoryRepository categoryRepository, StoreService storeService) {
        this.storeRepository = storeRepository;
        this.reservationService = reservationService;
        this.favoriteService = favoriteService;
        this.reviewRepository = reviewRepository;
        this.categoryRepository = categoryRepository;
        this.storeService = storeService;
    }

    // 店舗一覧を表示するメソッド
    @GetMapping
    public String index(@RequestParam(name = "keyword", required = false) String keyword,
                        @RequestParam(name = "price", required = false) Integer price,
                        @RequestParam(name = "order", required = false) String order,
                        @PageableDefault(page = 0, size = 10) Pageable pageable,
                        Model model) {
        boolean isPaidMember = isPaidMember(); // 会員ステータスを確認

        // 無料会員または未ログインの場合、レビュー関連のソートを無効化
        if (!isPaidMember && ("reviewHigh".equals(order) || "reviewLow".equals(order))) {
            order = "createdAtDesc"; // デフォルトの新着順に切り替え
        }

        Page<Store> storePage;

        // 検索条件とソート条件に応じて店舗を取得
        if ("reviewHigh".equals(order) || "reviewLow".equals(order)) {
            storePage = storeService.getStoresSortedByRating(order, pageable);
        } else if (keyword != null && !keyword.isEmpty()) {
            if ("priceAsc".equals(order)) {
                storePage = storeRepository.findByStoreNameLikeOrDescriptionLikeOrderByPriceAsc("%" + keyword + "%", "%" + keyword + "%", pageable);
            } else {
                storePage = storeRepository.findByStoreNameLikeOrDescriptionLikeOrderByCreatedAtDesc("%" + keyword + "%", "%" + keyword + "%", pageable);
            }
        } else if (price != null) {
            if ("priceAsc".equals(order)) {
                storePage = storeRepository.findByPriceLessThanEqualOrderByPriceAsc(price, pageable);
            } else {
                storePage = storeRepository.findByPriceLessThanEqualOrderByCreatedAtDesc(price, pageable);
            }
        } else {
            if ("priceAsc".equals(order)) {
                storePage = storeRepository.findAllByOrderByPriceAsc(pageable);
            } else {
                storePage = storeRepository.findAllByOrderByCreatedAtDesc(pageable);
            }
        }

        // 店舗が見つからない場合、空のページを設定
        if (storePage == null || storePage.isEmpty()) {
            storePage = Page.empty(pageable);
        }

        // テンプレートにデータを渡す
        model.addAttribute("storePage", storePage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("price", price);
        model.addAttribute("order", order);

        List<Category> categories = categoryRepository.findAll(); // カテゴリ情報を取得
        model.addAttribute("categories", categories);

        model.addAttribute("isPaidMember", isPaidMember); // 会員ステータスをテンプレートに渡す

        return "stores/index";
    }

    // 店舗詳細を表示するメソッド
    @GetMapping("/{id}")
    public String show(@PathVariable(name = "id") Long id, Model model) {
        Optional<Store> storeOptional = storeRepository.findById(id); // 店舗をIDで検索
        if (storeOptional.isPresent()) {
            Store store = storeOptional.get();
            model.addAttribute("store", store);
            model.addAttribute("reservationInputForm", new ReservationInputForm()); // 新規予約フォームを設定

            List<Review> reviews = reviewRepository.findByStoreId(store.getStoreId()); // レビューを取得
            model.addAttribute("reviews", reviews);

            double averageRating = reviews.stream() // 平均評価を計算
                                          .mapToInt(Review::getRating)
                                          .average()
                                          .orElse(0.0);
            String formattedAverageRating = averageRating > 0 ? String.format("%.1f", averageRating) : "N/A"; // フォーマット
            model.addAttribute("averageRatingFormatted", formattedAverageRating);

            model.addAttribute("isPaidMember", isPaidMember()); // 会員ステータスを渡す
            model.addAttribute("favorited", isFavorited(store.getStoreId())); // お気に入り情報を渡す
        } else {
            model.addAttribute("errorMessage", "指定された店舗が見つかりません。"); // エラー処理
            return "stores/error";
        }

        return "stores/show";
    }

    // 店舗に関連するレビューをページネーションで表示
    @GetMapping("/{storeId}/show")
    public String showStore(@PathVariable(name = "storeId") Long storeId,
                            @RequestParam(name = "page", defaultValue = "0") int page,
                            Model model) {
        int pageSize = 10; // 1ページあたりのレビュー数を指定
        Pageable pageable = PageRequest.of(page, pageSize); // ページング設定

        Page<Review> reviewPage = reviewRepository.findPagedByStoreId(storeId, pageable); // ページネーションされたレビューを取得

        double averageRating = reviewPage.getContent().stream() // 平均評価を計算
                                         .mapToInt(Review::getRating)
                                         .average()
                                         .orElse(0.0);

        String averageRatingFormatted = String.format("%.1f", averageRating); // 平均評価をフォーマット
        model.addAttribute("averageRatingFormatted", averageRatingFormatted);

        model.addAttribute("reviewPage", reviewPage); // レビューデータをテンプレートに渡す
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", reviewPage.getTotalPages());

        return "stores/show";
    }

    // 予約を作成するメソッド
    @PostMapping("/{id}/reserve")
    public String createReservation(@Valid ReservationInputForm form, BindingResult bindingResult, Model model, @PathVariable("id") Long storeId, RedirectAttributes redirectAttributes) {
        try {
            Optional<Store> storeOptional = storeRepository.findById(storeId); // 店舗を検索
            if (!storeOptional.isPresent()) {
                model.addAttribute("errorMessage", "指定された店舗が見つかりません。");
                return "stores/error";
            }

            Store store = storeOptional.get();
            model.addAttribute("store", store);
            model.addAttribute("isPaidMember", isPaidMember());

            if (bindingResult.hasErrors()) { // 入力エラーの検証
                model.addAttribute("errorMessage", "入力にエラーがあります。");
                model.addAttribute("validationErrors", bindingResult.getAllErrors());
                return "stores/show";
            }

            String errorMessage = reservationService.isReservationDateTimeValid(store, form.getReservationDatetime()); // 予約日時の検証
            if (errorMessage != null) {
                model.addAttribute("errorMessage", errorMessage);
                return "stores/show";
            }

            Reservation reservation = new Reservation(); // 新しい予約を作成
            reservation.setReservationDatetime(Timestamp.valueOf(form.getReservationDatetime()));
            reservation.setNumberOfPeople(form.getNumberOfPeople());
            reservation.setStore(store);

            MemberDetailsImpl memberDetails = (MemberDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal(); // 現在のユーザー情報を取得
            reservationService.create(reservation, memberDetails.getMember().getId(), storeId); // 予約を保存

            redirectAttributes.addAttribute("reserved", true); // リダイレクト先に成功フラグを渡す
            return "redirect:/reservations";
        } catch (Exception e) { // 例外処理
            e.printStackTrace();
            model.addAttribute("errorMessage", "予期しないエラーが発生しました。もう一度お試しください。");
            return "stores/show";
        }
    }

    // 現在のユーザーが有料会員かどうかを確認するヘルパーメソッド
    private boolean isPaidMember() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof MemberDetailsImpl) {
            MemberDetailsImpl userDetails = (MemberDetailsImpl) principal;
            return userDetails.isPaidMember();
        }

        return false;
    }

    // 現在のユーザーが店舗をお気に入り登録しているか確認するヘルパーメソッド
    private boolean isFavorited(Long storeId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof MemberDetailsImpl) {
            MemberDetailsImpl userDetails = (MemberDetailsImpl) principal;
            return favoriteService.isFavorited(userDetails.getMember(), storeId);
        }

        return false;
    }

    // カテゴリ検索用メソッド
    @GetMapping("/search")
    public String searchByCategory(@RequestParam("category") Long categoryId, 
                                   @RequestParam(name = "page", defaultValue = "0") int page,
                                   Model model, 
                                   Principal principal) {
        if (principal == null) { // ログインしていない場合のエラー処理
            model.addAttribute("error", "検索機能を利用するにはログインが必要です。");
            return "stores/index";
        }

        Pageable pageable = PageRequest.of(page, 10); // ページング設定

        Page<Store> storePage = storeRepository.findByCategories_CategoryId(categoryId, pageable); // カテゴリに基づく店舗検索

        model.addAttribute("storePage", storePage);
        model.addAttribute("categories", categoryRepository.findAll()); // カテゴリリストを渡す
        model.addAttribute("selectedCategory", categoryId);

        return "stores/index";
    }
}