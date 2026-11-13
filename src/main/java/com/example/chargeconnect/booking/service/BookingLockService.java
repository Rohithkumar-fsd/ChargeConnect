package com.example.chargeconnect.booking.service;

import java.time.Duration;

public interface BookingLockService {

    /**
     * Attempts to acquire an exclusive lock for a given slot key.
     *
     * @param lockKey   Unique key for the charger time slot
     * @param lockValue Value identifying the lock owner (e.g., driver email or booking ref)
     * @param ttl       Time-to-live before auto-expiry
     * @return true if acquired, false if already locked by another checkout
     */
    boolean tryAcquireLock(String lockKey, String lockValue, Duration ttl);

    /**
     * Releases the lock if the value matches the lock owner.
     *
     * @param lockKey   Unique key for the charger time slot
     * @param lockValue Value identifying the lock owner
     */
    void releaseLock(String lockKey, String lockValue);
}

