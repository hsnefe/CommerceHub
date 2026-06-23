package com.commercehub.auth.service;

import com.commercehub.auth.config.EmailVerificationProperties;
import com.commercehub.auth.dto.LoginRequest;
import com.commercehub.auth.dto.RegisterRequest;
import com.commercehub.auth.dto.TokenResponse;
import com.commercehub.auth.dto.UserResponse;
import com.commercehub.auth.entity.Role;
import com.commercehub.auth.entity.User;
import com.commercehub.auth.exception.ConflictException;
import com.commercehub.auth.exception.NotFoundException;
import com.commercehub.auth.exception.UnauthorizedException;
import com.commercehub.auth.repository.RoleRepository;
import com.commercehub.auth.repository.UserRepository;
import com.commercehub.auth.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenService tokenService;
    private final EmailVerificationProperties emailVerificationProperties;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            TokenService tokenService,
            EmailVerificationProperties emailVerificationProperties
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenService = tokenService;
        this.emailVerificationProperties = emailVerificationProperties;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already registered");
        }

        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new NotFoundException("Default USER role not found"));

        User user = new User();
        user.setEmail(request.email().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setEmailVerified(false);
        user.setRoles(Set.of(userRole));

        User saved = userRepository.save(user);
        return toUserResponse(saved);
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email().toLowerCase())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        if (emailVerificationProperties.required() && !user.isEmailVerified()) {
            throw new UnauthorizedException("Email not verified");
        }

        return buildTokenResponse(user);
    }

    @Transactional
    public TokenResponse refresh(String rawRefreshToken) {
        User user = tokenService.validateAndGetUser(rawRefreshToken);
        String newRefreshToken = tokenService.rotateRefreshToken(rawRefreshToken);
        return new TokenResponse(
                jwtService.generateAccessToken(user),
                newRefreshToken,
                jwtService.getAccessTokenExpirationSeconds()
        );
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        tokenService.revokeToken(rawRefreshToken);
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return toUserResponse(user);
    }

    private TokenResponse buildTokenResponse(User user) {
        return new TokenResponse(
                jwtService.generateAccessToken(user),
                tokenService.createRefreshToken(user),
                jwtService.getAccessTokenExpirationSeconds()
        );
    }

    private UserResponse toUserResponse(User user) {
        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .sorted()
                .toList();
        return new UserResponse(user.getId(), user.getEmail(), roles);
    }
}
