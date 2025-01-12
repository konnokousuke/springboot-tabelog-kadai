package com.example.nagoyameshi.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.nagoyameshi.entity.Review;
import com.example.nagoyameshi.security.MemberDetailsImpl;
import com.example.nagoyameshi.service.ReviewService;

@Controller
@RequestMapping("/reviews/history")
public class ReviewHistoryController {

    private final ReviewService reviewService;

    public ReviewHistoryController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    public String getReviewHistory(
            @PageableDefault(size = 10) Pageable pageable,
            Model model) {
        // ログインユーザーを取得
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        MemberDetailsImpl memberDetails = (MemberDetailsImpl) authentication.getPrincipal();
        Integer memberId = memberDetails.getMember().getId();

        // ログインユーザーのレビュー履歴を取得
        Page<Review> reviews = reviewService.getReviewsByMemberId(memberId, pageable);

        model.addAttribute("reviews", reviews);

        return "reviews/history";
    }
}
