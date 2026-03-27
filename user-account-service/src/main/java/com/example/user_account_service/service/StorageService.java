package com.example.user_account_service.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class StorageService {

    private final Path fileStorageLocation;

    public StorageService() {
        Path resolvedPath = null;
        try {
            resolvedPath = resolveUploadDirectory();
            // Đảm bảo thư mục 'uploads' tồn tại
            if (!Files.exists(resolvedPath)) {
                Files.createDirectories(resolvedPath);
            }
        } catch (Exception ex) {
            // Nếu chưa gán được path, tránh sử dụng field final
            throw new RuntimeException("Không thể tạo thư mục lưu trữ uploads", ex);
        }
        this.fileStorageLocation = resolvedPath;
    }

    /**
     * Lưu file và trả về tên file duy nhất (để lưu vào database)
     */
    public String storeFile(MultipartFile file, Long userId) {
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || file.isEmpty()) {
            throw new RuntimeException("File tải lên không hợp lệ.");
        }

        String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        String uniqueFileName = userId + "_" + UUID.randomUUID().toString() + fileExtension;

        try {
            Path targetLocation = this.fileStorageLocation.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), targetLocation, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return uniqueFileName;
        } catch (IOException ex) {
            throw new RuntimeException("Không thể lưu file " + originalFileName, ex);
        }
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