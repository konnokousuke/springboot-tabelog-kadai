package com.example.nagoyameshi.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.nagoyameshi.entity.Member;

public class MemberDetailsImpl implements UserDetails {
    private static final long serialVersionUID = 1L;

    private Member member;
    private Collection<? extends GrantedAuthority> authorities;

    public MemberDetailsImpl(Member member, Collection<? extends GrantedAuthority> authorities) {
        this.member = member;
        this.authorities = authorities;
    }
    
    public MemberDetailsImpl(Member member) {
    	this.member = member;
    	// デフォルトでロール情報を設定
    	this.authorities = List.of(new SimpleGrantedAuthority(member.getRole().getName()));
    	}

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return member.getPassword();
    }

    @Override
    public String getUsername() {
        return member.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return member.isEnabled();
    }

    public Member getMember() {
        return member;
    }

    public boolean isPaidMember() {
        return member.isPaidMember();
    }
}
