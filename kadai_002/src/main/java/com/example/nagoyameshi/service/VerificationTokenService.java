package com.example.nagoyameshi.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.nagoyameshi.entity.Member;
import com.example.nagoyameshi.entity.VerificationToken;
import com.example.nagoyameshi.repository.VerificationTokenRepository;

@Service
public class VerificationTokenService {

    private final VerificationTokenRepository verificationTokenRepository;

    public VerificationTokenService(VerificationTokenRepository verificationTokenRepository) {
        this.verificationTokenRepository = verificationTokenRepository;
    }

    // トークンの生成と保存
    public VerificationToken createVerificationToken(Member member) {
        String token = UUID.randomUUID().toString();

        // 既存のトークンを取得する
        Optional<VerificationToken> existingTokenOpt = verificationTokenRepository.findByMember(member);

        if (existingTokenOpt.isPresent()) {
            // 既存のトークンが存在する場合は更新する
            VerificationToken existingToken = existingTokenOpt.get();
            existingToken.setToken(token);
            return verificationTokenRepository.save(existingToken);  // 更新処理
        } else {
            // 既存のトークンが存在しない場合は新規作成
            return create(member, token);
        }
    }


    // トークンの取得
    public VerificationToken getVerificationToken(String token) {
        return verificationTokenRepository.findByToken(token);
    }
    
    // create メソッド
    public VerificationToken create(Member member, String token) {
        VerificationToken verificationToken = new VerificationToken(token, member);
        return verificationTokenRepository.save(verificationToken);
    }
}
