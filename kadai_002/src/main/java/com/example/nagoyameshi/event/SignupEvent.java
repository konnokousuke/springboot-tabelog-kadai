package com.example.nagoyameshi.event;

import org.springframework.context.ApplicationEvent;

import com.example.nagoyameshi.entity.Member;

import lombok.Getter;

@Getter
public class SignupEvent extends ApplicationEvent {
	private Member member;
	private String requestUrl;
	
	public SignupEvent(Object source, Member member, String requestUrl) {
		super(source);
		
		this.member = member;
		this.requestUrl = requestUrl;
	}
}
