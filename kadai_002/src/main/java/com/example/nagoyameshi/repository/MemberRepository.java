package com.example.nagoyameshi.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.nagoyameshi.entity.Member;

public interface MemberRepository extends JpaRepository<Member, Integer> {
	public Member findByEmail(String email);
	public Page<Member> findByNameLikeOrFuriganaLike(String nameKeyword, String furiganaKeyword, Pageable pageable);
	public Member findByCustomerId(String customerId);
}
