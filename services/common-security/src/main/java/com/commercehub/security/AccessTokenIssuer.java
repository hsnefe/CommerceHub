package com.commercehub.security;

import java.util.List;
import java.util.UUID;

public interface AccessTokenIssuer {

    String generateAccessToken(UUID userId, List<String> roles);

    long getAccessTokenExpirationSeconds();
}
