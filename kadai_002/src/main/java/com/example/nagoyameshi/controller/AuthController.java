package com.example.nagoyameshi.controller;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.nagoyameshi.entity.Member;
import com.example.nagoyameshi.entity.VerificationToken;
import com.example.nagoyameshi.event.SignupEventPublisher;
import com.example.nagoyameshi.form.SignupForm;
import com.example.nagoyameshi.service.EmailService;
import com.example.nagoyameshi.service.MemberService;
import com.example.nagoyameshi.service.VerificationTokenService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class AuthController {
    private final MemberService memberService;
    private final SignupEventPublisher signupEventPublisher;
    private final VerificationTokenService verificationTokenService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    
    public AuthController(MemberService memberService, SignupEventPublisher signupEventPublisher, VerificationTokenService verificationTokenService, PasswordEncoder passwordEncoder, EmailService emailService) {
        this.memberService = memberService;
        this.signupEventPublisher = signupEventPublisher;
        this.verificationTokenService = verificationTokenService;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/signup")
    public String signup(Model model) {
        model.addAttribute("signupForm", new SignupForm());
        return "auth/signup"; // 無料会員用登録ページ
    }

    @PostMapping("/signup")
    public String signup(@ModelAttribute @Validated SignupForm signupForm, BindingResult bindingResult, RedirectAttributes redirectAttributes, HttpServletRequest httpServletRequest) {
        // メールアドレスが登録済みであれば、BindingResultオブジェクトにエラー内容を追加する
        if (memberService.isEmailRegistered(signupForm.getEmail())) {
            FieldError fieldError = new FieldError(bindingResult.getObjectName(), "email", "すでに登録済みのメールアドレスです。");
            bindingResult.addError(fieldError);
        }

        // パスワードとパスワード(確認用)の入力内容が一致していなければ、BindingResultオブジェクトにエラー内容を追加する
        if (!memberService.isSamePassword(signupForm.getPassword(), signupForm.getPasswordConfirmation())) {
            FieldError fieldError = new FieldError(bindingResult.getObjectName(), "password", "パスワードが一致しません。");
            bindingResult.addError(fieldError);
        }

        if (bindingResult.hasErrors()) {
            return "auth/signup";
        }

        Member createdMember = memberService.create(signupForm);
        String requestUrl = httpServletRequest.getRequestURL().toString();
        signupEventPublisher.publishSignupEvent(createdMember, requestUrl);
        redirectAttributes.addFlashAttribute("sendEmailMessage", "ご入力いただいたメールアドレスに認証メールを送信しました。メールに記載されているリンクをクリックし、会員登録を完了してください。");

        return "redirect:/";
    }

    @GetMapping("signup/verify")
    public String verify(@RequestParam(name = "token") String token, Model model) {
        VerificationToken verificationToken = verificationTokenService.getVerificationToken(token);
        String key = null;
        String message = null;

        if (verificationToken != null) {
            Member member = verificationToken.getMember();
            memberService.enableMember(member);
            key = "successMessage";
            message = "会員登録が完了しました。";
        } else {
            key = "errorMessage";
            message = "トークンが無効です。";
        }
        model.addAttribute(key, message);
        return "auth/verify";
    }
    
    // 仮パスワード送信フォーム
    @GetMapping("/forgot-password")
    public String forgotPasswordForm() {
        return "auth/forgot-password";
    }
    
    // 仮パスワード送信
    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestParam("email") String email, RedirectAttributes redirectAttributes) {
        String tempPassword = UUID.randomUUID().toString().substring(0, 8);
        Member member = memberService.findByEmail(email);

        if (member != null) {
            member.setPassword(passwordEncoder.encode(tempPassword));
            memberService.save(member);

            // 仮パスワードをメールで送信
            emailService.sendSimpleMessage(email, "仮パスワード", "仮パスワードは: " + tempPassword);

            // 成功メッセージを追加してリダイレクト
            redirectAttributes.addFlashAttribute("successMessage", "仮パスワードを送信しました。");
            return "redirect:/reset-password";
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "登録されたメールアドレスが見つかりません。");
            return "redirect:/forgot-password";
        }
    }

    // パスワード再設定フォーム
    @GetMapping("/reset-password")
    public String resetPasswordForm() {
        return "auth/reset-password";
    }
    
    // パスワード再設定
    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam("email") String email,
                                @RequestParam("tempPassword") String tempPassword,
                                @RequestParam("newPassword") String newPassword,
                                @RequestParam("confirmPassword") String confirmPassword,
                                RedirectAttributes redirectAttributes) {
        // メールアドレスでユーザーを検索
        Member member = memberService.findByEmail(email);

        if (member == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "メールアドレスが登録されていません。");
            return "redirect:/reset-password";
        }

        // 仮パスワードの確認
        if (!passwordEncoder.matches(tempPassword, member.getPassword())) {
            redirectAttributes.addFlashAttribute("errorMessage", "仮パスワードが間違っています。");
            return "redirect:/reset-password";
        }

        // 新しいパスワードの一致確認
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "新しいパスワードが一致しません。");
            return "redirect:/reset-password";
        }
        
        // 新しいパスワードの長さを確認
        if (newPassword.length() < 8) {
            redirectAttributes.addFlashAttribute("errorMessage", "パスワードは8文字以上で入力してください。");
            return "redirect:/reset-password";
        }

        // パスワードを更新
        member.setPassword(passwordEncoder.encode(newPassword));
        memberService.save(member);

        // 成功メッセージをリダイレクト先に渡す
        redirectAttributes.addFlashAttribute("successMessage", "パスワードが再設定されました。");
        return "redirect:/login";
    }
}
