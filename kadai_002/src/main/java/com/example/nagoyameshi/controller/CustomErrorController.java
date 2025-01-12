package com.example.nagoyameshi.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.web.bind.annotation.RequestMapping;

public class CustomErrorController implements ErrorController {
	
	@RequestMapping("/error")
    public String handleError() {
        // カスタムエラーページのテンプレートを返す
        return "error";
    }

}
