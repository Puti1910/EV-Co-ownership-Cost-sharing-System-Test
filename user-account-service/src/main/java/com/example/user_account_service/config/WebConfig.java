package com.example.user_account_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadDir = resolveUploadDirectory();
        String uploadPath = uploadDir.toUri().toString();

        // Map URL /uploads/** tới thư mục vật lý uploads
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath)
                .setCachePeriod(3600);
    }

    private Path resolveUploadDirectory() {
        // Luôn trỏ tới: EV-Co-ownership-Cost-sharing-System/EV-Co-ownership-Cost-sharing-System/user-account-service/uploads
        Path currentDir = Paths.get("").toAbsolutePath(); // ví dụ: D:\Merge\EV-Co-ownership-Cost-sharing-System
        Path uploadPath = currentDir
                .resolve("EV-Co-ownership-Cost-sharing-System")
                .resolve("user-account-service")
                .resolve("uploads");

        return uploadPath.normalize();
    }
}


