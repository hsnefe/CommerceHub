package com.commercehub.auth.service;

import com.commercehub.auth.config.RefreshTokenProperties;
import com.commercehub.auth.entity.User;
import com.commercehub.auth.exception.UnauthorizedException;
import com.commercehub.auth.repository.UserRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class TokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String KEY_PREFIX = "refresh:";

    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;
    private final RefreshTokenProperties refreshTokenProperties;

    public TokenService(
            StringRedisTemplate redisTemplate,
            UserRepository userRepository,
            RefreshTokenProperties refreshTokenProperties) {
        this.redisTemplate = redisTemplate;
        this.userRepository = userRepository;
        this.refreshTokenProperties = refreshTokenProperties;
    }

    public String createRefreshToken(User user) {
        String rawToken = generateRawToken();
        String hash = hashToken(rawToken);
        long ttlSeconds = refreshTokenProperties.expirationDays() * 24L * 60L * 60L;
        redisTemplate.opsForValue().set(
                key(hash),
                user.getId().toString(),
                Duration.ofSeconds(ttlSeconds)
        );
        return rawToken;
    }

    public User validateAndGetUser(String rawToken) {
        String userId = redisTemplate.opsForValue().get(key(hashToken(rawToken)));
        if (userId == null) {
            throw new UnauthorizedException("Invalid refresh token");
        }
        return userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
    }

    public void revokeToken(String rawToken) {
        redisTemplate.delete(key(hashToken(rawToken)));
    }

    public String rotateRefreshToken(String rawToken) {
        String hash = hashToken(rawToken);
        String userId = redisTemplate.opsForValue().get(key(hash));
        if (userId == null) {
            throw new UnauthorizedException("Invalid refresh token");
        }
        redisTemplate.delete(key(hash));
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
        return createRefreshToken(user);
    }

    public String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private String key(String tokenHash) {
        return KEY_PREFIX + tokenHash;
    }

    private String generateRawToken() {
        byte[] bytes = new byte[48];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
