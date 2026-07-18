package com.commercehub.order.dto;

public record SendNotificationRequest(
        String email,
        String subject,
        String message
) {
}
