package com.example.nagoyameshi.security;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.nagoyameshi.entity.Member;
import com.example.nagoyameshi.repository.MemberRepository;

@Service
public class MemberDetailsServiceImpl implements UserDetailsService {
	
	private final MemberRepository memberRepository;
	
	public MemberDetailsServiceImpl(MemberRepository memberRepository) {
		this.memberRepository = memberRepository;
		}
	
	@Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Member member = memberRepository.findByEmail(email);
        if (member == null) {
            throw new UsernameNotFoundException("ユーザーが見つかりませんでした。: " + email);
        }
        String role = member.getRole().getName();
        Collection<GrantedAuthority> authorities = new ArrayList<>();         
        authorities.add(new SimpleGrantedAuthority(role));
        return new MemberDetailsImpl(member, authorities);
    }
	
}
