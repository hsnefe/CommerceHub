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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String PASSWORD = "password123";
    private static final String EMAIL = "user@example.com";
    private static final String ACCESS_TOKEN = "access-token";
    private static final String REFRESH_TOKEN = "refresh-token";
    private static final String NEW_REFRESH_TOKEN = "new-refresh-token";
    private static final long EXPIRES_IN = 3600L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private TokenService tokenService;

    @Mock
    private EmailVerificationProperties emailVerificationProperties;

    @InjectMocks
    private AuthService authService;

    private Role userRole() {
        Role role = new Role();
        role.setId(UUID.randomUUID());
        role.setName("USER");
        return role;
    }

    private Role adminRole() {
        Role role = new Role();
        role.setId(UUID.randomUUID());
        role.setName("ADMIN");
        return role;
    }

    private User sampleUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(EMAIL);
        user.setPasswordHash("hashed");
        user.setEmailVerified(false);
        user.setRoles(new HashSet<>(Set.of(userRole())));
        return user;
    }

    private void stubJwtAndTokenServices(User user) {
        when(jwtService.generateAccessToken(user)).thenReturn(ACCESS_TOKEN);
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(EXPIRES_IN);
        when(tokenService.createRefreshToken(user)).thenReturn(REFRESH_TOKEN);
    }

    @Nested
    class Register {

        @Test
        void register_success_returnsUserResponse() {
            Role role = userRole();
            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(roleRepository.findByName("USER")).thenReturn(Optional.of(role));
            when(passwordEncoder.encode(PASSWORD)).thenReturn("hashed");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(UUID.randomUUID());
                return user;
            });

            UserResponse response = authService.register(new RegisterRequest(EMAIL, PASSWORD));

            assertThat(response.id()).isNotNull();
            assertThat(response.email()).isEqualTo(EMAIL);
            assertThat(response.roles()).containsExactly("USER");
            verify(userRepository).save(any(User.class));
        }

        @Test
        void register_duplicateEmail_throwsConflictException() {
            when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

            assertThatThrownBy(() -> authService.register(new RegisterRequest(EMAIL, PASSWORD)))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Email already registered");
        }

        @Test
        void register_missingUserRole_throwsNotFoundException() {
            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(roleRepository.findByName("USER")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.register(new RegisterRequest(EMAIL, PASSWORD)))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Default USER role not found");
        }

        @Test
        void register_normalizesEmailToLowerCase() {
            String mixedCaseEmail = "User@Example.COM";
            Role role = userRole();
            when(userRepository.existsByEmail(mixedCaseEmail)).thenReturn(false);
            when(roleRepository.findByName("USER")).thenReturn(Optional.of(role));
            when(passwordEncoder.encode(PASSWORD)).thenReturn("hashed");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(UUID.randomUUID());
                return user;
            });

            authService.register(new RegisterRequest(mixedCaseEmail, PASSWORD));

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getEmail()).isEqualTo("user@example.com");
        }

        @Test
        void register_setsEmailVerifiedFalseAndEncodesPassword() {
            Role role = userRole();
            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(roleRepository.findByName("USER")).thenReturn(Optional.of(role));
            when(passwordEncoder.encode(PASSWORD)).thenReturn("hashed");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(UUID.randomUUID());
                return user;
            });

            authService.register(new RegisterRequest(EMAIL, PASSWORD));

            verify(passwordEncoder).encode(PASSWORD);
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User saved = userCaptor.getValue();
            assertThat(saved.isEmailVerified()).isFalse();
            assertThat(saved.getPasswordHash()).isEqualTo("hashed");
        }
    }

    @Nested
    class Login {

        @Test
        void login_success_returnsTokenResponse() {
            User user = sampleUser();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(PASSWORD, user.getPasswordHash())).thenReturn(true);
            when(emailVerificationProperties.required()).thenReturn(false);
            stubJwtAndTokenServices(user);

            TokenResponse response = authService.login(new LoginRequest(EMAIL, PASSWORD));

            assertThat(response.accessToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(response.refreshToken()).isEqualTo(REFRESH_TOKEN);
            assertThat(response.expiresIn()).isEqualTo(EXPIRES_IN);
        }

        @Test
        void login_unknownEmail_throwsUnauthorizedException() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, PASSWORD)))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Invalid credentials");
        }

        @Test
        void login_wrongPassword_throwsUnauthorizedException() {
            User user = sampleUser();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(PASSWORD, user.getPasswordHash())).thenReturn(false);

            assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, PASSWORD)))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Invalid credentials");
        }

        @Test
        void login_looksUpEmailInLowerCase() {
            String mixedCaseEmail = "Login@Example.com";
            String normalizedEmail = "login@example.com";
            User user = sampleUser();
            user.setEmail(normalizedEmail);
            when(userRepository.findByEmail(normalizedEmail)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(PASSWORD, user.getPasswordHash())).thenReturn(true);
            when(emailVerificationProperties.required()).thenReturn(false);
            stubJwtAndTokenServices(user);

            authService.login(new LoginRequest(mixedCaseEmail, PASSWORD));

            verify(userRepository).findByEmail(normalizedEmail);
        }

        @Test
        void login_whenVerificationRequiredAndUnverified_throwsUnauthorizedException() {
            User user = sampleUser();
            user.setEmailVerified(false);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(PASSWORD, user.getPasswordHash())).thenReturn(true);
            when(emailVerificationProperties.required()).thenReturn(true);

            assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, PASSWORD)))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Email not verified");
        }

        @Test
        void login_whenVerificationRequiredAndVerified_succeeds() {
            User user = sampleUser();
            user.setEmailVerified(true);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(PASSWORD, user.getPasswordHash())).thenReturn(true);
            when(emailVerificationProperties.required()).thenReturn(true);
            stubJwtAndTokenServices(user);

            TokenResponse response = authService.login(new LoginRequest(EMAIL, PASSWORD));

            assertThat(response.accessToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(response.refreshToken()).isEqualTo(REFRESH_TOKEN);
        }

        @Test
        void login_whenVerificationNotRequired_succeedsForUnverifiedUser() {
            User user = sampleUser();
            user.setEmailVerified(false);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(PASSWORD, user.getPasswordHash())).thenReturn(true);
            when(emailVerificationProperties.required()).thenReturn(false);
            stubJwtAndTokenServices(user);

            TokenResponse response = authService.login(new LoginRequest(EMAIL, PASSWORD));

            assertThat(response.accessToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(response.refreshToken()).isEqualTo(REFRESH_TOKEN);
        }
    }

    @Nested
    class Refresh {

        @Test
        void refresh_success_returnsNewTokenResponse() {
            User user = sampleUser();
            when(tokenService.validateAndGetUser(REFRESH_TOKEN)).thenReturn(user);
            when(tokenService.rotateRefreshToken(REFRESH_TOKEN)).thenReturn(NEW_REFRESH_TOKEN);
            when(jwtService.generateAccessToken(user)).thenReturn(ACCESS_TOKEN);
            when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(EXPIRES_IN);

            TokenResponse response = authService.refresh(REFRESH_TOKEN);

            assertThat(response.accessToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(response.refreshToken()).isEqualTo(NEW_REFRESH_TOKEN);
            assertThat(response.expiresIn()).isEqualTo(EXPIRES_IN);
            verify(tokenService).validateAndGetUser(REFRESH_TOKEN);
            verify(tokenService).rotateRefreshToken(REFRESH_TOKEN);
        }

        @Test
        void refresh_propagatesTokenServiceException() {
            when(tokenService.validateAndGetUser(REFRESH_TOKEN))
                    .thenThrow(new UnauthorizedException("Invalid refresh token"));

            assertThatThrownBy(() -> authService.refresh(REFRESH_TOKEN))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Invalid refresh token");
            verifyNoMoreInteractions(tokenService);
        }
    }

    @Nested
    class Logout {

        @Test
        void logout_delegatesToTokenService() {
            authService.logout(REFRESH_TOKEN);

            verify(tokenService).revokeToken(REFRESH_TOKEN);
        }

        @Test
        void logout_completesWithoutException() {
            authService.logout(REFRESH_TOKEN);

            verify(tokenService).revokeToken(eq(REFRESH_TOKEN));
        }
    }

    @Nested
    class GetCurrentUser {

        @Test
        void getCurrentUser_success_returnsUserResponse() {
            User user = sampleUser();
            UUID userId = user.getId();
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            UserResponse response = authService.getCurrentUser(userId);

            assertThat(response.id()).isEqualTo(userId);
            assertThat(response.email()).isEqualTo(EMAIL);
            assertThat(response.roles()).containsExactly("USER");
        }

        @Test
        void getCurrentUser_unknownId_throwsNotFoundException() {
            UUID unknownId = UUID.randomUUID();
            when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.getCurrentUser(unknownId))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("User not found");
        }

        @Test
        void getCurrentUser_rolesAreSortedAlphabetically() {
            User user = sampleUser();
            user.setRoles(new HashSet<>(Set.of(userRole(), adminRole())));
            UUID userId = user.getId();
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            UserResponse response = authService.getCurrentUser(userId);

            assertThat(response.roles()).containsExactly("ADMIN", "USER");
        }
    }
}
