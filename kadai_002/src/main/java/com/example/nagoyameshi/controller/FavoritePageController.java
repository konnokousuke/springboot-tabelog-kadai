package com.example.nagoyameshi.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.nagoyameshi.entity.Favorite;
import com.example.nagoyameshi.entity.Member;
import com.example.nagoyameshi.security.MemberDetailsImpl;
import com.example.nagoyameshi.service.FavoriteService;

@Controller
public class FavoritePageController {

    private final FavoriteService favoriteService;

    @Autowired
    public FavoritePageController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping("/favorites")
    public String favoritesPage(
            Model model,
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @PageableDefault(size = 10) Pageable pageable) {
        if (memberDetails == null) {
            return "redirect:/login"; // ログインしていない場合はログインページへリダイレクト
        }

        Member member = memberDetails.getMember();
        Page<Favorite> favoritePage = favoriteService.findFavoritesByMember(member, pageable);

        model.addAttribute("favoritePage", favoritePage); // テンプレートにデータを渡す
        return "favorites/list"; // favorites/list.html をレンダリング
    }
}
