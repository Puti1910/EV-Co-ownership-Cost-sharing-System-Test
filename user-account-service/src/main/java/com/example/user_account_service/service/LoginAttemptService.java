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
        log.info("Resetting failed attempts for key: [{}]", key);
        attemptsCache.remove(key);
    }

    public void loginFailed(String key) {
        CachedValue value = attemptsCache.getOrDefault(key, new CachedValue(0, System.currentTimeMillis()));
        value.count++;
        value.lastAttemptTime = System.currentTimeMillis();
        attemptsCache.put(key, value);
        log.warn("Login FAILED for key: [{}]. Fail count = [{}/{}]", key, value.count, MAX_ATTEMPT);
    }

    public boolean isBlocked(String key) {
        CachedValue value = attemptsCache.get(key);
        if (value == null) {
            return false;
        }
        if (System.currentTimeMillis() - value.lastAttemptTime > BLOCK_DURATION) {
            log.info("Block EXPIRED for key: [{}]", key);
            attemptsCache.remove(key);
            return false;
        }
        boolean blocked = value.count >= MAX_ATTEMPT;
        if (blocked) {
            log.warn("Key is BLOCKED: [{}] (Count: {})", key, value.count);
        }
        return blocked;
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
