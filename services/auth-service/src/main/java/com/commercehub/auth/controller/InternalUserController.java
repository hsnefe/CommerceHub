package com.commercehub.auth.controller;

import com.commercehub.auth.dto.InternalUserResponse;
import com.commercehub.auth.dto.UserResponse;
import com.commercehub.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/users")
@Hidden
public class InternalUserController {

    private final AuthService authService;

    public InternalUserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/{userId}")
    public InternalUserResponse getUser(@PathVariable UUID userId) {
        UserResponse user = authService.getCurrentUser(userId);
        return new InternalUserResponse(user.id(), user.email());
    }
}
