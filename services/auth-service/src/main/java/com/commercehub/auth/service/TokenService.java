package com.commercehub.auth.service;

import com.commercehub.auth.config.RefreshTokenProperties;
import com.commercehub.auth.entity.RefreshToken;
import com.commercehub.auth.entity.User;
import com.commercehub.auth.exception.UnauthorizedException;
import com.commercehub.auth.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class TokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenProperties refreshTokenProperties;

    public TokenService(RefreshTokenRepository refreshTokenRepository, RefreshTokenProperties refreshTokenProperties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenProperties = refreshTokenProperties;
    }

    @Transactional
    public String createRefreshToken(User user) {
        String rawToken = generateRawToken();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(hashToken(rawToken));
        refreshToken.setExpiresAt(Instant.now().plusSeconds(refreshTokenProperties.expirationDays() * 24L * 60L * 60L));
        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    @Transactional
    public User validateAndGetUser(String rawToken) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hashToken(rawToken))
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (!stored.isActive()) {
            throw new UnauthorizedException("Refresh token is expired or revoked");
        }

        return stored.getUser();
    }

    @Transactional
    public void revokeToken(String rawToken) {
        refreshTokenRepository.findByTokenHash(hashToken(rawToken))
                .ifPresent(token -> {
                    token.setRevokedAt(Instant.now());
                    refreshTokenRepository.save(token);
                });
    }

    @Transactional
    public String rotateRefreshToken(String rawToken) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hashToken(rawToken))
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (!stored.isActive()) {
            throw new UnauthorizedException("Refresh token is expired or revoked");
        }

        stored.setRevokedAt(Instant.now());
        refreshTokenRepository.save(stored);
        return createRefreshToken(stored.getUser());
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

    private String generateRawToken() {
        byte[] bytes = new byte[48];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
