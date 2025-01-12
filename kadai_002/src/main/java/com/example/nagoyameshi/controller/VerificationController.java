package com.example.nagoyameshi.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.nagoyameshi.entity.Member;
import com.example.nagoyameshi.service.MemberService;

public class VerificationController {

    private final MemberService memberService;

    public VerificationController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("/verify")
    public String verifyAccount(@RequestParam("token") String token, RedirectAttributes redirectAttributes) {
        Member verifiedMember = memberService.verifyToken(token);

        if (verifiedMember != null) {
            redirectAttributes.addFlashAttribute("successMessage", "有料会員登録が完了しました。");
            return "redirect:/member-info";
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "無効なトークンです。");
            return "redirect:/error";
        }
    }

}