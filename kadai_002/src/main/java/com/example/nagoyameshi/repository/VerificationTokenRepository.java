package com.example.nagoyameshi.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.nagoyameshi.entity.Member;
import com.example.nagoyameshi.entity.VerificationToken;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    
    // Memberに紐づくトークンをOptionalで取得するメソッドを定義
    Optional<VerificationToken> findByMember(Member member);

    // トークンで検索するメソッド（既存のもの）
    VerificationToken findByToken(String token);
}
