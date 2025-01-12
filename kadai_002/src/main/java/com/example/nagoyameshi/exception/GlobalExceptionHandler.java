package com.example.nagoyameshi.exception;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    // 一般的な例外を処理するためのメソッド
    @ExceptionHandler(Exception.class)
    public String handleException(Exception ex, Model model) {
        model.addAttribute("errorMessage", "予期しないエラーが発生しました。もう一度お試しください。");
        return "error"; // エラーページにリダイレクト
    }

    // 404エラー（ページが見つからない）の処理
    @ExceptionHandler(NoHandlerFoundException.class)
    public String handleNoHandlerFoundException(NoHandlerFoundException ex, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", "ページが見つかりませんでした。");
        return "redirect:/error"; // エラーページにリダイレクト
    }

}
