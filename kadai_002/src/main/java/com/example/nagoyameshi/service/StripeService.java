package com.example.nagoyameshi.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.nagoyameshi.entity.Member;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentMethod;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PaymentMethodListParams;
import com.stripe.param.SubscriptionCreateParams;
import com.stripe.param.checkout.SessionCreateParams;

@Service
public class StripeService {
   
	@Value("${app.url}") // アプリケーションURLを取得
    private String appUrl;
	
    @Value("${stripe.api-key}")
    private String stripeApiKey;

    // コンストラクタでStripe APIキーを初期化
    public StripeService(@Value("${stripe.api-key}") String stripeApiKey) {
        Stripe.apiKey = stripeApiKey; // Stripe APIの初期化をコンストラクタで行う
    }

    // 顧客の作成
    public Customer createCustomer(Member member) throws StripeException {
        CustomerCreateParams params = CustomerCreateParams.builder()
                .setEmail(member.getEmail())
                .setName(member.getName())
                .setPhone(member.getPhoneNumber())
                .build();

        return Customer.create(params);
    }

    // サブスクリプションを作成
    public Subscription createSubscription(Member member, String priceId) throws StripeException {
        // Stripe顧客を作成（または既存の顧客を利用）
        Customer customer = createCustomer(member);

        SubscriptionCreateParams params = SubscriptionCreateParams.builder()
                .setCustomer(customer.getId()) // 作成した顧客IDを設定
                .addItem(SubscriptionCreateParams.Item.builder()
                        .setPrice(priceId) // 価格IDを設定
                        .build())
                .build();

        return Subscription.create(params);
    }

    // checkout.session.completedイベントの処理
    public void processSessionCompleted(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();

        if (deserializer.getObject().isPresent()) {
            StripeObject stripeObject = deserializer.getObject().get();
            if (stripeObject instanceof Session) {
                Session session = (Session) stripeObject;

                String customerId = session.getCustomer();
                String subscriptionId = session.getSubscription();

                // 顧客IDとサブスクリプションIDの処理
                System.out.println("Customer ID: " + customerId);
                System.out.println("Subscription ID: " + subscriptionId);
            }
        } else {
            // デシリアライズ失敗時のエラーハンドリング
            System.out.println("Failed to deserialize Stripe object.");
        }
    }

    // Stripe Checkoutセッションを作成
    public Session createCheckoutSession(Member member, String priceId) throws StripeException {
        // 顧客の作成
        Customer customer = createCustomer(member);
        
        // 正しいURLを構築
        String successUrl = appUrl.replaceAll("/+$", "") + "/stripe/success";
        String cancelUrl = appUrl.replaceAll("/+$", "") + "/auth/cancel";

        // Checkoutセッションの作成
        SessionCreateParams params = SessionCreateParams.builder()
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD) // カード決済を許可
                .setCustomer(customer.getId()) // 顧客IDを設定
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION) // サブスクリプションモードを設定
                .setSuccessUrl(successUrl) // 環境に応じたsuccess_url
                .setCancelUrl(cancelUrl) // 環境に応じたcancel_url
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPrice(priceId) // 価格IDを設定
                        .setQuantity(1L) // 数量を設定
                        .build())
                .build();

        return Session.create(params); // セッションを作成
    }

    // セッションIDからSessionオブジェクトを取得
    public Session retrieveSession(String sessionId) throws StripeException {
        return Session.retrieve(sessionId); // セッションIDからSessionを取得
    }

    // 顧客のクレジットカード情報を削除
    public void deleteCustomerCard(String customerId) throws StripeException {
        // 必須パラメータを設定
        PaymentMethodListParams params = PaymentMethodListParams.builder()
                .setCustomer(customerId) // 対象顧客IDを指定
                .setType(PaymentMethodListParams.Type.CARD) // 支払い方法のタイプを指定（enumを使用）
                .build();

        // 支払い方法をリストとして取得
        List<PaymentMethod> paymentMethods = PaymentMethod.list(params).getData();

        // 登録されているすべてのカードを削除
        for (PaymentMethod pm : paymentMethods) {
            pm.detach();
        }
    }

    // サブスクリプションのキャンセル
    public void cancelSubscription(String customerId) throws StripeException {
        // 顧客に関連するサブスクリプションを検索
        Map<String, Object> params = new HashMap<>();
        params.put("customer", customerId);
        List<Subscription> subscriptions = Subscription.list(params).getData();

        // 顧客の全てのサブスクリプションをキャンセル
        for (Subscription subscription : subscriptions) {
            Subscription updatedSubscription = subscription.cancel();
            System.out.println("Subscription canceled: " + updatedSubscription.getId());
        }
    }

    // クレジットカード更新セッションの作成
    public String createCardUpdateSession(String customerId, String returnUrl) throws StripeException {
        SessionCreateParams params = SessionCreateParams.builder()
                .setCustomer(customerId)
                .setMode(SessionCreateParams.Mode.SETUP) // カード情報の設定モード
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .setSuccessUrl(returnUrl) // 成功時のリダイレクト先
                .setCancelUrl(returnUrl) // キャンセル時のリダイレクト先
                .build();

        Session session = Session.create(params);
        return session.getUrl(); // 更新URLを返す
    }
    
    public String updateCustomerCard(String customerId, String returnUrl) throws StripeException {
        // 既存のカードを削除
        deleteCustomerCard(customerId);

        // 新しいカードのセットアップセッションを作成
        SessionCreateParams params = SessionCreateParams.builder()
                .setCustomer(customerId)
                .setMode(SessionCreateParams.Mode.SETUP) // カード情報の設定モード
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .setSuccessUrl(returnUrl) // 成功時のリダイレクト先
                .setCancelUrl(returnUrl) // キャンセル時のリダイレクト先
                .build();

        Session session = Session.create(params);
        
     // カード情報を更新した場合の状態を返す
        return session.getUrl();

    }
}
