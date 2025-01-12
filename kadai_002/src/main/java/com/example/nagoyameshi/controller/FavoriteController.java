package com.example.nagoyameshi.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.nagoyameshi.entity.Favorite;
import com.example.nagoyameshi.entity.Member;
import com.example.nagoyameshi.security.MemberDetailsImpl;
import com.example.nagoyameshi.service.FavoriteService;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    @Autowired
    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @PostMapping("/add/{storeId}")
    public ResponseEntity<?> addFavorite(@AuthenticationPrincipal MemberDetailsImpl userDetails, @PathVariable Long storeId) {
        try {
            favoriteService.addFavorite(userDetails.getMember(), storeId);
            return ResponseEntity.ok(Map.of("message", "お気に入り登録が完了しました"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "お気に入り登録に失敗しました"));
        }
    }

    @DeleteMapping("/remove/{storeId}")
    public ResponseEntity<?> removeFavorite(@AuthenticationPrincipal MemberDetailsImpl userDetails, @PathVariable Long storeId) {
        try {
            favoriteService.removeFavorite(userDetails.getMember(), storeId);
            return ResponseEntity.ok(Map.of("message", "お気に入りを解除しました"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "お気に入り解除に失敗しました"));
        }
    }

    @GetMapping("/check/{storeId}")
    public ResponseEntity<Boolean> isFavorited(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @PathVariable Long storeId) {
        try {
            Member member = memberDetails.getMember();
            boolean isFavorited = favoriteService.isFavorited(member, storeId);
            return ResponseEntity.ok(isFavorited);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(false);
        }
    }

    @GetMapping("/list")
    public ResponseEntity<Page<Map<String, Object>>> listFavorites(
            @PageableDefault(size = 10) Pageable pageable,
            @AuthenticationPrincipal MemberDetailsImpl memberDetails) {
        try {
            if (memberDetails == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            Member member = memberDetails.getMember();
            Page<Favorite> favoritePage = favoriteService.findFavoritesByMember(member, pageable);

            // Favoriteエンティティを変換して`isFavorited`フラグを付与
            Page<Map<String, Object>> resultPage = favoritePage.map(favorite -> Map.of(
                "favoriteId", favorite.getFavoriteId(),
                "store", favorite.getStore(),
                "isFavorited", true, // 常にtrue（お気に入りリストだから）
                "createdAt", favorite.getCreatedAt(),
                "updatedAt", favorite.getUpdatedAt()
            ));

            return ResponseEntity.ok(resultPage);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}
