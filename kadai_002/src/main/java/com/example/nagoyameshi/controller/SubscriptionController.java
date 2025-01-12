package com.example.nagoyameshi.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.nagoyameshi.entity.Member;
import com.example.nagoyameshi.service.MemberService;
import com.example.nagoyameshi.service.StripeService;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;

@Controller
@RequestMapping("/subscription")
public class SubscriptionController {

    private final StripeService stripeService;
    private final MemberService memberService;

    @Value("${stripe.price.id}")
    private String priceId;

    public SubscriptionController(StripeService stripeService, MemberService memberService) {
        this.stripeService = stripeService;
        this.memberService = memberService;
    }

    /**
     * Stripe Checkoutセッションを作成し、フロントエンドにURLを返すエンドポイント
     */
    @PostMapping("/session")
    @ResponseBody
    public Map<String, Object> createCheckoutSession() {
        Map<String, Object> response = new HashMap<>();
        try {
            // ログインしている現在のユーザーを取得
            Member member = getCurrentLoggedInMember();
            if (member == null) {
                throw new IllegalArgumentException("ログインユーザーが見つかりません。");
            }

            // StripeのCheckoutセッションを作成
            Session session = stripeService.createCheckoutSession(member, priceId);
            
            // セッションURLをフロントエンドに返す
            response.put("url", session.getUrl());
        } catch (StripeException e) {
            // エラーハンドリングとログ記録
            e.printStackTrace();
            response.put("error", "現在サブスクリプションサービスが利用できません。しばらくしてからもう一度お試しください。");
        }
        return response;
    }

    /**
     * ログイン中のユーザーを取得するメソッド
     */
    private Member getCurrentLoggedInMember() {
        // セキュリティコンテキストからユーザー情報を取得
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        return memberService.findByEmail(currentUserEmail);
    }
}
