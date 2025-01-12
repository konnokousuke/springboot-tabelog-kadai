package com.example.nagoyameshi.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class NavigationController {

    @GetMapping("/navigation/about")
    public String about(Model model) throws IOException {
        // プロジェクト内のstatic/storageディレクトリを取得
        String storagePath = Paths.get("src/main/resources/static/storage").toAbsolutePath().toString();
        File storageDir = new File(storagePath);
        File[] files = storageDir.listFiles();

        List<String> imagePaths = new ArrayList<>();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().matches(".*\\.(jpg|png|jpeg|gif)$")) {
                    imagePaths.add("/storage/" + file.getName());
                }
            }
        }

        // 画像リストをシャッフル
        Collections.shuffle(imagePaths);
        model.addAttribute("imagePaths", imagePaths);

        return "navigation/about"; // templates/navigation/about.html を返す
    }

    @GetMapping("/navigation/company")
    public String company() {
        return "navigation/company"; // templates/navigation/company.html を返す
    }

    @GetMapping("/navigation/terms")
    public String terms() {
        return "navigation/terms"; // templates/navigation/terms.html を返す
    }

    @GetMapping("/navigation/faq")
    public String faq() {
        return "navigation/faq"; // templates/navigation/faq.html を返す
    }
}
