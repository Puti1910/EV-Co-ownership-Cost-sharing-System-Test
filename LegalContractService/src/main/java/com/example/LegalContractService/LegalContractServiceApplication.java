package com.example.LegalContractService;  // ✅ CHÚ Ý: package phải giống y hệt controller gốc

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.example.LegalContractService") // ✅ quét toàn bộ controller/service
@EnableJpaRepositories(basePackages = "com.example.LegalContractService.repository") // ✅ Enable JPA Repositories
@EntityScan(basePackages = "com.example.LegalContractService.model") // ✅ Scan Entity classes
public class LegalContractServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LegalContractServiceApplication.class, args);
        System.out.println("🚗 LegalContractService started on port 8089 ✅");
    }
}
