package com.example.nagoyameshi.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Constraint(validatedBy = NotEmptyFileValidator.class) // Validatorクラスを指定
@Target({ ElementType.FIELD }) // フィールドに適用可能
@Retention(RetentionPolicy.RUNTIME) // 実行時に有効
public @interface NotEmptyFile {
    String message() default "画像を選択してください。"; // エラーメッセージ
    Class<?>[] groups() default {}; // グループ定義
    Class<? extends Payload>[] payload() default {}; // ペイロード定義
}
