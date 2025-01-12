package com.example.nagoyameshi.controller;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.nagoyameshi.entity.Member;
import com.example.nagoyameshi.security.MemberDetailsImpl;
import com.example.nagoyameshi.service.MemberService;
import com.example.nagoyameshi.service.StripeService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/stripe")
public class StripeWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(StripeWebhookController.class);
    private final MemberService memberService;
    private final StripeService stripeService;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @Value("${app.url}")
    private String appUrl;

    public StripeWebhookController(MemberService memberService, StripeService stripeService) {
        this.memberService = memberService;
        this.stripeService = stripeService;
    }

    @GetMapping("/card/update-url")
    public ResponseEntity<Map<String, String>> getCardUpdateUrl(@AuthenticationPrincipal MemberDetailsImpl memberDetails) {
        try {
            Member member = memberDetails.getMember();

            String updateUrl = stripeService.updateCustomerCard(member.getCustomerId(), appUrl.replaceAll("/+$", "") + "/member/edit");

            Map<String, String> response = new HashMap<>();
            response.put("url", updateUrl);
            response.put("message", "クレジットカード情報を変更しました。ログインし直してください。");

            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            logger.error("Failed to generate card update URL", e);

            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "クレジットカード情報の更新に失敗しました。");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {
        logger.info("Received Stripe Webhook with payload: {}", payload);
        logger.info("Stripe-Signature header: {}", sigHeader);

        try {
            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            logger.info("Stripe Event type: {}", event.getType());

            switch (event.getType()) {
                case "checkout.session.completed":
                    handleCheckoutSessionCompleted(event);
                    break;
                case "setup_intent.succeeded":
                    handleCardUpdateCompleted(event);
                    break;
                case "customer.subscription.deleted":
                    handleSubscriptionDeleted(event);
                    break;
                default:
                    logger.info("Unhandled event type: {}", event.getType());
                    break;
            }

            return ResponseEntity.ok("Webhook processed successfully");
        } catch (SignatureVerificationException e) {
            logger.error("Stripe signature verification failed: {}", e.getMessage());
            return ResponseEntity.status(400).body("Invalid Stripe signature");
        } catch (Exception e) {
            logger.error("Error processing webhook", e);
            return ResponseEntity.status(500).body("Internal Server Error");
        }
    }

    private void handleCheckoutSessionCompleted(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject = deserializer.getObject().orElse(null);

        if (stripeObject instanceof Session) {
            Session session = (Session) stripeObject;

            String customerId = session.getCustomer();
            String email = session.getCustomerDetails().getEmail();

            logger.info("Customer ID from session: {}", customerId);
            logger.info("Email from session: {}", email);

            Member member = memberService.findByEmail(email);

            if (member == null) {
                logger.error("Member not found for email: {}", email);
                return;
            }

            member.setStatus(Member.Status.PAID);
            member.setCustomerId(customerId);
            memberService.updateRoleToPaid(member);
            memberService.saveAndFlush(member);

            logger.info("有料会員登録が完了しました。ログインし直してください。");
        } else {
            logger.warn("Session data is null");
        }
    }

    private void handleCardUpdateCompleted(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject = deserializer.getObject().orElse(null);

        if (stripeObject instanceof Session) {
            Session session = (Session) stripeObject;

            String customerId = session.getCustomer();
            logger.info("Card update completed for customer ID: {}", customerId);

            Member member = memberService.findByCustomerId(customerId);

            if (member != null) {
                logger.info("Card update completed for member: {}", member.getEmail());
            } else {
                logger.warn("No member found for customer ID: {}", customerId);
            }
        } else {
            logger.warn("Card update session data is null");
        }
    }

    private void handleSubscriptionDeleted(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject = deserializer.getObject().orElse(null);

        if (stripeObject instanceof Subscription) {
            Subscription subscription = (Subscription) stripeObject;

            String customerId = subscription.getCustomer();
            logger.info("Subscription deleted for customer ID: {}", customerId);

            Member member = memberService.findByCustomerId(customerId);

            if (member == null) {
                logger.error("Member not found for customer ID: {}", customerId);
                return;
            }

            member.setStatus(Member.Status.FREE);
            member.setCustomerId(null);
            memberService.downgradeToFreeMember(member);
            memberService.saveAndFlush(member);

            logger.info("Member {} downgraded to FREE membership", member.getEmail());
        } else {
            logger.warn("Subscription data is null");
        }
    }
    
    @GetMapping("/success")
    public String handleSuccess(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        // セッションを無効化してログアウトを実行
        request.getSession().invalidate();

        // リダイレクト時にフラッシュ属性でメッセージを設定
        redirectAttributes.addFlashAttribute("successMessage", "有料会員登録が完了しました。");

        // ホームにリダイレクト
        return "redirect:/";
    }

}