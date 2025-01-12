package com.example.nagoyameshi.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfig {

    private final MemberDetailsServiceImpl memberDetailsServiceImpl;
    private final AdminDetailsServiceImpl adminDetailsServiceImpl;

    public WebSecurityConfig(MemberDetailsServiceImpl memberDetailsServiceImpl, AdminDetailsServiceImpl adminDetailsServiceImpl) {
        this.memberDetailsServiceImpl = memberDetailsServiceImpl;
        this.adminDetailsServiceImpl = adminDetailsServiceImpl;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/stripe/webhook", "/paid_signup/session") // Stripe Webhook用にCSRFを無視する設定
            )
            .exceptionHandling((exceptions) -> exceptions
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN); // 403ステータス
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"error\": \"Access Denied\"}");
                })
            )
            .authorizeHttpRequests((requests) -> requests
                .requestMatchers(
                    "/css/**", "/images/**", "/js/**", "/storage/**", "/", 
                    "/signup/**", "/stores", "/stores/{id}", "/paid_signup/**", 
                    "/auth/paid_signup/**", "/stripe/webhook", "/paid_confirm", 
                    "/success", "/forgot-password", "/reset-password",
                    "/navigation/about", "/navigation/company", "/navigation/terms", "/navigation/faq",
                    "/stripe/success", "/member/edit" 
                ).permitAll() // すべてのユーザーにアクセスを許可するURL
                .requestMatchers("/member/downgrade/confirm", "/member/delete").authenticated() // 解約処理は認証されたユーザーのみ許可
                .requestMatchers("/admin/**").hasRole("ADMIN") // 管理者のみアクセスを許可するURL
                .requestMatchers("/paid/**", "/api/favorites/**", "/stores/{storeId}/reviews/**").hasRole("PAID") // 有料会員のみアクセスを許可するURL
                .anyRequest().authenticated() // 上記以外のURLはログインが必要
            )
            .formLogin((form) -> form
                .loginPage("/login") // ログインページのURL
                .loginProcessingUrl("/login") // ログインフォームの送信先URL
                .defaultSuccessUrl("/?loggedIn") // ログイン成功時のリダイレクト先URL
                .failureUrl("/login?error") // ログイン失敗時のリダイレクト先URL
                .permitAll()
            )
            .logout((logout) -> logout
                .logoutUrl("/logout") // ログアウトエンドポイントのURL
                .logoutSuccessUrl("/?loggedOut") // ログアウト時のリダイレクト先URL
                .invalidateHttpSession(true) // セッションを無効化
                .deleteCookies("JSESSIONID") // クッキーを削除
                .permitAll()
            )
            .userDetailsService(memberDetailsServiceImpl)
            .userDetailsService(adminDetailsServiceImpl);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}