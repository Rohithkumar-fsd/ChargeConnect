package com.example.chargeconnect.booking.service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisBookingLockService implements BookingLockService {

    private static final Logger log = LoggerFactory.getLogger(RedisBookingLockService.class);

    private final StringRedisTemplate redisTemplate;
    private final ConcurrentHashMap<String, InMemoryLock> fallbackLocks = new ConcurrentHashMap<>();

    public RedisBookingLockService(@Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        if (redisTemplate == null) {
            log.warn("StringRedisTemplate bean not available. Using resilient in-memory lock store.");
        }
    }

    @Override
    public boolean tryAcquireLock(String lockKey, String lockValue, Duration ttl) {
        if (redisTemplate != null) {
            try {
                Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, ttl);
                return Boolean.TRUE.equals(acquired);
            } catch (Exception ex) {
                log.warn("Redis operation failed ('{}'), falling back to in-memory lock store: {}", lockKey, ex.getMessage());
            }
        }

        // In-memory fallback
        Instant now = Instant.now();
        cleanExpiredLocks(now);

        InMemoryLock existing = fallbackLocks.get(lockKey);
        if (existing != null && existing.expiresAt().isAfter(now)) {
            return false;
        }

        fallbackLocks.put(lockKey, new InMemoryLock(lockValue, now.plus(ttl)));
        return true;
    }

    @Override
    public void releaseLock(String lockKey, String lockValue) {
        if (redisTemplate != null) {
            try {
                String current = redisTemplate.opsForValue().get(lockKey);
                if (lockValue.equals(current)) {
                    redisTemplate.delete(lockKey);
                }
                return;
            } catch (Exception ex) {
                log.warn("Redis delete failed ('{}'), falling back to in-memory lock release: {}", lockKey, ex.getMessage());
            }
        }

        InMemoryLock existing = fallbackLocks.get(lockKey);
        if (existing != null && lockValue.equals(existing.value())) {
            fallbackLocks.remove(lockKey);
        }
    }

    private void cleanExpiredLocks(Instant now) {
        fallbackLocks.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    private record InMemoryLock(String value, Instant expiresAt) {}
}

