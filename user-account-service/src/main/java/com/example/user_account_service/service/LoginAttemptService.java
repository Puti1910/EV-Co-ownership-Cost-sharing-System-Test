package com.example.user_account_service.service;

import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class LoginAttemptService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LoginAttemptService.class);
    
    private final int MAX_ATTEMPT = 5;
    private final long BLOCK_DURATION = TimeUnit.MINUTES.toMillis(15);
    private final ConcurrentHashMap<String, CachedValue> attemptsCache = new ConcurrentHashMap<>();

    public void loginSucceeded(String key) {
        log.info(">>> [SECURITY] Đăng nhập thành công. Reset bộ đếm cho Key: [{}]", key);
        attemptsCache.remove(key);
    }

    public void loginFailed(String key) {
        attemptsCache.compute(key, (k, v) -> {
            if (v == null) {
                log.warn(">>> [SECURITY] Lần đầu tiên đăng nhập sai cho Key: [{}]", k);
                return new CachedValue(1, System.currentTimeMillis());
            }
            v.count++;
            v.lastAttemptTime = System.currentTimeMillis();
            log.warn(">>> [SECURITY] Đăng nhập sai lần thứ [{}/{}] cho Key: [{}]", v.count, MAX_ATTEMPT, k);
            return v;
        });
    }

    public boolean isBlocked(String key) {
        CachedValue value = attemptsCache.get(key);
        if (value == null) return false;

        // Kiểm tra xem đã hết thời gian chặn chưa
        if (System.currentTimeMillis() - value.lastAttemptTime > BLOCK_DURATION) {
            log.info(">>> [SECURITY] Hết thời gian chặn cho Key: [{}]. Reset bộ đếm.", key);
            attemptsCache.remove(key);
            return false;
        }

        if (value.count >= MAX_ATTEMPT) {
            log.error(">>> [SECURITY] CHẶN truy cập cho Key: [{}] (Sai {} lần)", key, value.count);
            return true;
        }
        return false;
    }


    private static class CachedValue {
        int count;
        long lastAttemptTime;

        CachedValue(int count, long lastAttemptTime) {
            this.count = count;
            this.lastAttemptTime = lastAttemptTime;
        }
    }
}
