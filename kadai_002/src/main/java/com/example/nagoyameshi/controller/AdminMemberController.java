package com.example.nagoyameshi.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.nagoyameshi.entity.Member;
import com.example.nagoyameshi.repository.MemberRepository;

@Controller
@RequestMapping("/admin/members")
public class AdminMemberController {
	private final MemberRepository memberRepository;
	
	public AdminMemberController(MemberRepository memberRepository) {
		this.memberRepository = memberRepository;
	}
	
	@GetMapping
	public String index(@RequestParam(name = "keyword", required = false) String keyword, @PageableDefault(page = 0, size = 10, sort = "id", direction =Direction.ASC) Pageable pageable, Model model) {
		Page<Member> memberPage;
		
		if (keyword != null && !keyword.isEmpty()) {
			memberPage = memberRepository.findByNameLikeOrFuriganaLike("%" + keyword + "%", "%" + keyword + "%", pageable);
		} else {
			memberPage = memberRepository.findAll(pageable);
		}
		
		model.addAttribute("memberPage", memberPage);
		model.addAttribute("keyword", keyword);
		
		return "admin/members/index";
	}
	
	@GetMapping("/{id}")
	public String show(@PathVariable(name = "id") Integer id, Model model) {
		Member member = memberRepository.getReferenceById(id);
		
		model.addAttribute("member", member);
		
		return "admin/members/show";
	}
 }
