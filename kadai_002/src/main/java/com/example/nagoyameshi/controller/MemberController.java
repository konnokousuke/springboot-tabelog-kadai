package com.example.nagoyameshi.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.nagoyameshi.entity.Member;
import com.example.nagoyameshi.entity.Role;
import com.example.nagoyameshi.form.MemberEditForm;
import com.example.nagoyameshi.repository.MemberRepository;
import com.example.nagoyameshi.repository.RoleRepository;
import com.example.nagoyameshi.security.MemberDetailsImpl;
import com.example.nagoyameshi.service.MemberService;
import com.example.nagoyameshi.service.StripeService;
import com.stripe.exception.StripeException;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/member")
public class MemberController {

    private static final Logger logger = LoggerFactory.getLogger(MemberController.class);

    private final MemberRepository memberRepository;
    private final MemberService memberService;
    private final StripeService stripeService;
    private final RoleRepository roleRepository;

    public MemberController(MemberRepository memberRepository, MemberService memberService,
                            StripeService stripeService, RoleRepository roleRepository) {
        this.memberRepository = memberRepository;
        this.memberService = memberService;
        this.stripeService = stripeService;
        this.roleRepository = roleRepository;
    }

    // 認証済みユーザーのメンバーを取得する共通メソッド
    private Member getAuthenticatedMember(@AuthenticationPrincipal MemberDetailsImpl memberDetailsImpl) {
        return memberRepository.getReferenceById(memberDetailsImpl.getMember().getId());
    }

    // フラッシュメッセージを設定する共通メソッド
    private void setFlashMessage(RedirectAttributes redirectAttributes, String successMessage, String errorMessage) {
        if (successMessage != null) {
            redirectAttributes.addFlashAttribute("successMessage", successMessage);
        }
        if (errorMessage != null) {
            redirectAttributes.addFlashAttribute("errorMessage", errorMessage);
        }
    }

    @GetMapping
    public String index(@AuthenticationPrincipal MemberDetailsImpl memberDetailsImpl, Model model) {
        Member member = getAuthenticatedMember(memberDetailsImpl);
        model.addAttribute("member", member);

        String successMessage = (String) model.asMap().get("successMessage");
        if (successMessage == null || successMessage.isEmpty()) {
            if (member.getStatus() == Member.Status.PAID && memberService.isPaidRegistrationComplete(member.getEmail())) {
                successMessage = "有料会員登録が完了しました。ログインし直してください。";
                memberService.clearPaidRegistrationFlag(member.getEmail());
            }
        }

        logger.debug("successMessage = {}", successMessage);

        model.addAttribute("successMessage", successMessage != null ? successMessage : "");
        return "member/index";
    }

    @GetMapping("/edit")
    public String edit(@AuthenticationPrincipal MemberDetailsImpl memberDetailsImpl, Model model) {
        Member member = getAuthenticatedMember(memberDetailsImpl);
        MemberEditForm memberEditForm = new MemberEditForm(
            member.getId(), member.getName(), member.getFurigana(),
            member.getPostalCode(), member.getAddress(), member.getPhoneNumber(),
            member.getStatus().name()
        );
        model.addAttribute("memberEditForm", memberEditForm);
        return "member/edit";
    }

    @PostMapping("/update")
    public String update(@ModelAttribute @Validated MemberEditForm memberEditForm, BindingResult bindingResult,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "member/edit";
        }

        memberService.update(memberEditForm);
        redirectAttributes.addFlashAttribute("successMessage", "会員情報を編集しました。");
        return "redirect:/member";
    }

    @PostMapping("/subscribe/complete")
    public String completeSubscription(@AuthenticationPrincipal MemberDetailsImpl memberDetailsImpl,
                                       RedirectAttributes redirectAttributes) {
        try {
            memberService.completePaidRegistration(memberDetailsImpl.getMember().getEmail());
            redirectAttributes.addFlashAttribute("toastMessage", "有料会員登録が完了しました。ログインし直してください。");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "有料会員登録処理に失敗しました。再度お試しください。");
        }
        return "redirect:/member";
    }


    @Transactional
    @PostMapping("/downgrade/confirm")
    public String downgradeConfirm(@AuthenticationPrincipal MemberDetailsImpl memberDetailsImpl,
                                   RedirectAttributes redirectAttributes) {
        Member member = getAuthenticatedMember(memberDetailsImpl);

        try {
            if (member.getCustomerId() != null) {
                stripeService.cancelSubscription(member.getCustomerId());
                stripeService.deleteCustomerCard(member.getCustomerId());
                member.setCustomerId(null);
            }

            Role freeRole = roleRepository.findByName("ROLE_FREE");
            memberService.updateMemberStatus(member, Member.Status.FREE, freeRole);

            updateSession(member);
            setFlashMessage(redirectAttributes, "有料会員から無料会員への手続きが完了しました。", null);
        } catch (StripeException e) {
            setFlashMessage(redirectAttributes, null, "Stripeとの通信に失敗しました。再度お試しください。");
        } catch (Exception e) {
            setFlashMessage(redirectAttributes, null, "エラーが発生しました。もう一度お試しください。");
        }

        return "redirect:/member";
    }

    private void updateSession(Member member) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        MemberDetailsImpl newDetails = new MemberDetailsImpl(member);
        UsernamePasswordAuthenticationToken newAuth = new UsernamePasswordAuthenticationToken(
            newDetails, authentication.getCredentials(), newDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(newAuth);
    }
    
    // 無料会員解約処理
    @GetMapping("/member/edit")
    public String showEditPage(Authentication authentication, Model model) {
        // ログイン中の会員のメールアドレスを取得
        String email = authentication.getName();
        MemberEditForm memberEditForm = memberService.getMemberEditFormByEmail(email);

        // フォームをモデルに追加
        model.addAttribute("memberEditForm", memberEditForm);

        return "member/edit";
    }
    
    @Transactional
    @PostMapping("/delete")
    public String deleteMember(@AuthenticationPrincipal MemberDetailsImpl memberDetailsImpl,
                               RedirectAttributes redirectAttributes,
                               HttpServletRequest request, HttpServletResponse response) {
        // 現在のログイン中の会員を取得
        Member member = getAuthenticatedMember(memberDetailsImpl);

        try {
            // 会員情報を削除
            memberRepository.deleteById(member.getId());

            // ログアウトを実行
            SecurityContextHolder.clearContext();

            // セッション無効化
            request.getSession().invalidate();

            // クッキー削除（例: Spring SecurityのデフォルトのRemember-Meクッキーを削除）
            Cookie cookie = new Cookie("JSESSIONID", null);
            cookie.setPath("/");
            cookie.setMaxAge(0);
            response.addCookie(cookie);

            redirectAttributes.addFlashAttribute("successMessage", "解約処理が完了しました。");

            return "redirect:/login"; // ログイン画面にリダイレクト
        } catch (Exception e) {
            // エラーハンドリング
            logger.error("会員情報の削除中にエラーが発生しました", e);
            redirectAttributes.addFlashAttribute("errorMessage", "解約処理中にエラーが発生しました。もう一度お試しください。");
            return "redirect:/member";
        }
    }
}