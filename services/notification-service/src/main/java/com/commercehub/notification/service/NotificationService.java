package com.commercehub.notification.service;

import com.commercehub.notification.dto.SendNotificationRequest;
import com.commercehub.notification.dto.SendNotificationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    public SendNotificationResponse send(SendNotificationRequest request) {
        log.info(
                "Email sent to {} | subject={} | message={}",
                request.email(),
                request.subject(),
                request.message()
        );
        return new SendNotificationResponse(true);
    }
}
