package com.example.apigateway.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class RateLimitFilter implements GlobalFilter, Ordered {

    // Danh sách các thùng chứa (buckets) cho mỗi IP
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    // Cấu hình: 10 requests mỗi giây (Refill 10 tokens mỗi giây)
    private final Bandwidth limit = Bandwidth.classic(10, Refill.greedy(10, Duration.ofSeconds(1)));

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Lấy IP của người dùng
        String ip = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        
        // Lấy hoặc tạo thùng chứa cho IP này
        Bucket bucket = buckets.computeIfAbsent(ip, k -> Bucket.builder().addLimit(limit).build());

        // Kiểm tra xem còn token không
        if (bucket.tryConsume(1)) {
            return chain.filter(exchange);
        } else {
            log.warn(">>> [RATE_LIMIT] IP [{}] đã vượt quá giới hạn request!", ip);
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            exchange.getResponse().getHeaders().add("Content-Type", "application/json");
            
            String responseBody = "{\"error\": \"Too Many Requests\", \"message\": \"Bạn đã gửi quá nhiều yêu cầu. Vui lòng chậm lại một chút.\"}";
            org.springframework.core.io.buffer.DataBuffer buffer = exchange.getResponse()
                    .bufferFactory().wrap(responseBody.getBytes());
            
            return exchange.getResponse().writeWith(reactor.core.publisher.Mono.just(buffer));
        }
    }


    @Override
    public int getOrder() {
        // Chạy sớm nhất để chặn request ngay từ đầu
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
