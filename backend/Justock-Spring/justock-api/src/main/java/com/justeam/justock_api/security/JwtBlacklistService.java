package com.justeam.justock_api.security;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class JwtBlacklistService {

    private final ConcurrentHashMap<String, Long> blacklist = new ConcurrentHashMap<>();

    private static final long TTL_MS = 24 * 60 * 60 * 1000; // 24 hours

    public void addToBlacklist(String token, long expirationTime) {
        long expiryTime = expirationTime > System.currentTimeMillis() 
            ? expirationTime 
            : System.currentTimeMillis() + TTL_MS;
        blacklist.put(token, expiryTime);
    }

    public boolean isTokenBlacklisted(String token) {
        Long expiryTime = blacklist.get(token);
        if (expiryTime == null) {
            return false;
        }
        
        if (expiryTime < System.currentTimeMillis()) {
            blacklist.remove(token);
            return false;
        }
        
        return true;
    }

    public void cleanExpiredTokens() {
        long now = System.currentTimeMillis();
        blacklist.entrySet().removeIf(entry -> entry.getValue() < now);
    }
}
