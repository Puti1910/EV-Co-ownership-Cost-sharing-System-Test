package com.example.costpayment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CostPaymentServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CostPaymentServiceApplication.class, args);
	}

}
